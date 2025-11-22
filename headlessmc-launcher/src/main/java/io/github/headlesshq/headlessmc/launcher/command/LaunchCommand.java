package io.github.headlesshq.headlessmc.launcher.command;

import lombok.CustomLog;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.YesNoContext;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.launcher.auth.AuthException;
import io.github.headlesshq.headlessmc.launcher.auth.LaunchAccount;
import io.github.headlesshq.headlessmc.auth.ValidatedAccount;
import io.github.headlesshq.headlessmc.launcher.command.download.AbstractDownloadingVersionCommand;
import io.github.headlesshq.headlessmc.launcher.launch.LaunchException;
import io.github.headlesshq.headlessmc.launcher.launch.LaunchOptions;
import io.github.headlesshq.headlessmc.launcher.version.Version;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@CustomLog
public class LaunchCommand extends AbstractDownloadingVersionCommand {
    public LaunchCommand(Launcher launcher) {
        super(launcher, "launch", "Launches the game.");
        args.put("<version/id>", "Name or id of the version to launch. If you use the id you need to use the -id flag as well.");
        args.put("-id", "Use if you specified an id instead of a version name.");
        args.put("-commands", "Starts the game with the built-in command line support.");
        args.put("-lwjgl", "Removes lwjgl code, causing Minecraft not to render anything.");
        args.put("-inmemory", "Launches the game in the same JVM headlessmc is running in.");
        args.put("-jndi", "Patches the Log4J vulnerability.");
        args.put("-lookup", "Patches the Log4J vulnerability even harder.");
        args.put("-paulscode", "Removes some error messages from the PaulsCode library which may annoy you if you started the game with the -lwjgl flag.");
        args.put("-noout", "Doesn't print Minecrafts output to the console."); // TODO: is this really necessary?
        args.put("-quit", "Quit HeadlessMc after launching the game.");
        args.put("-offline", "Launch Mc in offline mode.");
        args.put("--jvm", "Jvm args to use.");
        args.put("--retries", "The amount of times you want to retry running Minecraft.");
    }

    @Override
    public void execute(Version version, String... args) throws CommandException {
        // 在启动前检查账户状态
        try {
            checkAccountBeforeLaunch();
        } catch (AuthException e) {
            // Token 失效，提示用户选择
            ctx.log("警告: " + e.getMessage());
            ctx.log("是否继续启动游戏？(y/n)");
            ctx.log("(注意: 使用失效的 token 启动游戏会导致'无效的会话'错误)");
            
            final boolean[] shouldContinue = {false};
            final boolean[] userResponded = {false};
            
            YesNoContext.goBackAfter(ctx, result -> {
                synchronized (shouldContinue) {
                    shouldContinue[0] = result;
                    userResponded[0] = true;
                    shouldContinue.notify();
                }
            });
            
            // 等待用户响应
            synchronized (shouldContinue) {
                while (!userResponded[0]) {
                    try {
                        shouldContinue.wait(30000); // 最多等待30秒
                        if (!userResponded[0]) {
                            throw new CommandException("等待用户响应超时");
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new CommandException("等待用户响应被中断");
                    }
                }
            }
            
            if (!shouldContinue[0]) {
                ctx.log("已取消启动。请使用 'yggdrasil login' 命令重新登录。");
                throw new CommandException("Token 失效，用户选择取消启动");
            }
            
            ctx.log("警告: 使用失效的 token 启动，游戏可能会显示'无效的会话'错误");
        }
        
        ClientLaunchProcessLifecycle lifecycle = new ClientLaunchProcessLifecycle(version, args);
        lifecycle.run(version);
    }
    
    /**
     * 在启动前检查账户状态
     * @throws AuthException 如果 token 失效
     */
    private void checkAccountBeforeLaunch() throws AuthException {
        ctx.log("========================================");
        ctx.log("开始检查账户状态...");
        
        // 检查 Yggdrasil 账户
        io.github.headlesshq.headlessmc.auth.YggdrasilAccount yggdrasilAccount = ctx.getAccountManager().getPrimaryYggdrasilAccount();
        if (yggdrasilAccount != null) {
            ctx.log("检测到 Yggdrasil 账户: " + yggdrasilAccount.getName());
            ctx.log("开始验证 token...");
            
            boolean isValid = ctx.getAccountManager().validateYggdrasilToken(yggdrasilAccount);
            
            if (!isValid) {
                ctx.log("========================================");
                ctx.log("【TOKEN 验证失败】");
                ctx.log("账户名称: " + yggdrasilAccount.getName());
                ctx.log("验证服务器: " + yggdrasilAccount.getServerUrl());
                ctx.log("");
                ctx.log("验证过程:");
                ctx.log("  1. 向服务器发送 POST 请求到: " + yggdrasilAccount.getServerUrl() + "/authserver/validate");
                ctx.log("  2. 请求包含 accessToken 和 clientToken");
                ctx.log("  3. 服务器返回非 204 状态码，表示 token 无效");
                ctx.log("");
                ctx.log("可能的原因:");
                ctx.log("  - Token 已过期（通常 Yggdrasil token 有有效期限制）");
                ctx.log("  - Token 已被服务器撤销");
                ctx.log("  - 网络连接问题导致无法验证");
                ctx.log("  - 服务器配置变更");
                ctx.log("");
                ctx.log("解决方案: 使用 'yggdrasil login' 命令重新登录以获取新的 token");
                ctx.log("========================================");
                throw new AuthException("Yggdrasil 账户 " + yggdrasilAccount.getName() + " 的 token 已失效！需要重新登录。");
            } else {
                ctx.log("Token 验证成功: 账户状态正常");
                ctx.log("========================================");
            }
        } else {
            ctx.log("未检测到 Yggdrasil 账户，跳过 token 验证");
            ctx.log("========================================");
        }
    }

    private class ClientLaunchProcessLifecycle extends AbstractLaunchProcessLifecycle {
        private final Version version;
        private @Nullable LaunchAccount account;

        public ClientLaunchProcessLifecycle(Version version, String[] args) {
            super(LaunchCommand.this.ctx, args);
            this.version = version;
        }

        @Override
        protected void getAccount() throws CommandException {
            this.account = LaunchCommand.this.getAccount();
        }

        @Override
        protected Path getGameDir() {
            return Paths.get(ctx.getConfig().get(LauncherProperties.GAME_DIR, ctx.getGameDir(version).getPath())).toAbsolutePath();
        }

        @Override
        protected @Nullable Process createProcess() throws LaunchException, AuthException, IOException {
            return ctx.getProcessFactory().run(
                    LaunchOptions.builder()
                            .account(account)
                            .version(version)
                            .launcher(ctx)
                            .files(files)
                            .closeCommandLine(!prepare)
                            .parseFlags(ctx, quit, args)
                            .prepare(prepare)
                            .build()
            );
        }
    }

    protected LaunchAccount getAccount() throws CommandException {
        try {
            // 优先使用 Microsoft 账户
            ValidatedAccount msaAccount = ctx.getAccountManager().getPrimaryAccount();
            if (msaAccount != null) {
                if (ctx.getConfig().get(LauncherProperties.REFRESH_ON_GAME_LAUNCH, true)) {
                    try {
                        msaAccount = ctx.getAccountManager().refreshAccount(msaAccount, ctx.getConfig());
                    } catch (AuthException e) {
                        if (ctx.getConfig().get(LauncherProperties.FAIL_LAUNCH_ON_REFRESH_FAILURE, false)) {
                            throw e;
                        }
                    }
                }
                return toLaunchAccount(msaAccount);
            }

            // 如果没有 Microsoft 账户，尝试使用 Yggdrasil 账户
            io.github.headlesshq.headlessmc.auth.YggdrasilAccount yggdrasilAccount = ctx.getAccountManager().getPrimaryYggdrasilAccount();
            if (yggdrasilAccount != null) {
                // 注意：token 验证在 execute 方法中进行，这里直接返回账户
                return toLaunchAccount(yggdrasilAccount);
            }

            // 如果都没有，检查离线模式
            if (ctx.getAccountManager().getOfflineChecker().isOffline()) {
                return ctx.getAccountManager().getOfflineAccount(ctx.getConfig());
            }

            throw new AuthException("You can't play the game without an account! Please use the login command.");
        } catch (AuthException e) {
            throw new CommandException(e.getMessage());
        }
    }

    private LaunchAccount toLaunchAccount(ValidatedAccount account) {
        return new LaunchAccount("msa",
                account.getSession().getMcProfile().getName(),
                account.getSession().getMcProfile().getId().toString(),
                account.getSession().getMcProfile().getMcToken().getAccessToken(),
                account.getXuid());
    }

    private LaunchAccount toLaunchAccount(io.github.headlesshq.headlessmc.auth.YggdrasilAccount account) throws AuthException {
        // 格式化 UUID（确保有连字符）
        String uuid = account.getUuid();
        if (uuid == null || uuid.isEmpty()) {
            throw new AuthException("Yggdrasil account UUID is missing!");
        }
        if (!uuid.contains("-")) {
            uuid = formatUuid(uuid);
        }
        
        String accessToken = account.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            throw new AuthException("Yggdrasil account access token is missing!");
        }
        
        String name = account.getName();
        if (name == null || name.isEmpty()) {
            throw new AuthException("Yggdrasil account name is missing!");
        }
        
        // 使用 "legacy" 作为 user_type，因为 Minecraft 客户端认识这个类型
        // "yggdrasil" 类型可能不被客户端识别
        return new LaunchAccount("legacy",
                name,
                uuid,
                accessToken,
                null); // Yggdrasil 没有 XUID
    }

    private String formatUuid(String uuid) {
        if (uuid.length() != 32) {
            return uuid; // 如果格式不对，直接返回
        }
        return uuid.substring(0, 8) + "-" +
               uuid.substring(8, 12) + "-" +
               uuid.substring(12, 16) + "-" +
               uuid.substring(16, 20) + "-" +
               uuid.substring(20, 32);
    }

}

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
        // Check account status before launch
        try {
            checkAccountBeforeLaunch();
        } catch (AuthException e) {
            // Token invalid, prompt user to choose
            ctx.log("Warning: " + e.getMessage() + " / 警告: " + e.getMessage());
            ctx.log("Continue to launch game? (y/n) / 是否继续启动游戏？(y/n)");
            ctx.log("(Note: Launching with invalid token will cause 'Invalid session' error) / (注意: 使用失效的 token 启动游戏会导致'无效的会话'错误)");
            
            final boolean[] shouldContinue = {false};
            final boolean[] userResponded = {false};
            
            YesNoContext.goBackAfter(ctx, result -> {
                synchronized (shouldContinue) {
                    shouldContinue[0] = result;
                    userResponded[0] = true;
                    shouldContinue.notify();
                }
            });
            
            // Wait for user response
            synchronized (shouldContinue) {
                while (!userResponded[0]) {
                    try {
                        shouldContinue.wait(30000); // Wait up to 30 seconds
                        if (!userResponded[0]) {
                            throw new CommandException("Timeout waiting for user response / 等待用户响应超时");
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new CommandException("Interrupted while waiting for user response / 等待用户响应被中断");
                    }
                }
            }
            
            if (!shouldContinue[0]) {
                ctx.log("Launch cancelled. Please use 'yggdrasil login' command to re-login. / 已取消启动。请使用 'yggdrasil login' 命令重新登录。");
                throw new CommandException("Token invalid, user chose to cancel launch / Token 失效，用户选择取消启动");
            }
            
            ctx.log("Warning: Launching with invalid token, game may show 'Invalid session' error / 警告: 使用失效的 token 启动，游戏可能会显示'无效的会话'错误");
        }
        
        ClientLaunchProcessLifecycle lifecycle = new ClientLaunchProcessLifecycle(version, args);
        lifecycle.run(version);
    }
    
    /**
     * Check account status before launch
     * 在启动前检查账户状态
     * @throws AuthException If token is invalid / 如果 token 失效
     */
    private void checkAccountBeforeLaunch() throws AuthException {
        ctx.log("========================================");
        ctx.log("Checking account status... / 开始检查账户状态...");
        
        // Check Yggdrasil account
        io.github.headlesshq.headlessmc.auth.YggdrasilAccount yggdrasilAccount = ctx.getAccountManager().getPrimaryYggdrasilAccount();
        if (yggdrasilAccount != null) {
            ctx.log("Detected Yggdrasil account: " + yggdrasilAccount.getName() + " / 检测到 Yggdrasil 账户: " + yggdrasilAccount.getName());
            ctx.log("Starting token validation... / 开始验证 token...");
            
            boolean isValid = ctx.getAccountManager().validateYggdrasilToken(yggdrasilAccount);
            
            if (!isValid) {
                ctx.log("========================================");
                ctx.log("【TOKEN VALIDATION FAILED / TOKEN 验证失败】");
                ctx.log("Account name: " + yggdrasilAccount.getName() + " / 账户名称: " + yggdrasilAccount.getName());
                ctx.log("Validation server: " + yggdrasilAccount.getServerUrl() + " / 验证服务器: " + yggdrasilAccount.getServerUrl());
                ctx.log("");
                ctx.log("Validation process: / 验证过程:");
                ctx.log("  1. Send POST request to: " + yggdrasilAccount.getServerUrl() + "/authserver/validate / 向服务器发送 POST 请求到: " + yggdrasilAccount.getServerUrl() + "/authserver/validate");
                ctx.log("  2. Request contains accessToken and clientToken / 请求包含 accessToken 和 clientToken");
                ctx.log("  3. Server returned non-204 status code, indicating token is invalid / 服务器返回非 204 状态码，表示 token 无效");
                ctx.log("");
                ctx.log("Possible reasons: / 可能的原因:");
                ctx.log("  - Token expired (Yggdrasil tokens usually have expiration limits) / Token 已过期（通常 Yggdrasil token 有有效期限制）");
                ctx.log("  - Token revoked by server / Token 已被服务器撤销");
                ctx.log("  - Network connection issue preventing validation / 网络连接问题导致无法验证");
                ctx.log("  - Server configuration changed / 服务器配置变更");
                ctx.log("");
                ctx.log("Solution: Use 'yggdrasil login' command to re-login and get a new token / 解决方案: 使用 'yggdrasil login' 命令重新登录以获取新的 token");
                ctx.log("========================================");
                throw new AuthException("Yggdrasil account " + yggdrasilAccount.getName() + " token is invalid! Please re-login. / Yggdrasil 账户 " + yggdrasilAccount.getName() + " 的 token 已失效！需要重新登录。");
            } else {
                ctx.log("Token validation successful: Account status is normal / Token 验证成功: 账户状态正常");
                ctx.log("========================================");
            }
        } else {
            ctx.log("No Yggdrasil account detected, skipping token validation / 未检测到 Yggdrasil 账户，跳过 token 验证");
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

            // If no Microsoft account, try Yggdrasil account
            io.github.headlesshq.headlessmc.auth.YggdrasilAccount yggdrasilAccount = ctx.getAccountManager().getPrimaryYggdrasilAccount();
            if (yggdrasilAccount != null) {
                // Note: Token validation is done in execute method, return account directly here
                return toLaunchAccount(yggdrasilAccount);
            }

            // If neither, check offline mode
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
        // Format UUID (ensure it has hyphens)
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
        
        // Use "legacy" as user_type because Minecraft client recognizes this type
        // "yggdrasil" type may not be recognized by the client
        return new LaunchAccount("legacy",
                name,
                uuid,
                accessToken,
                null); // Yggdrasil doesn't have XUID
    }

    private String formatUuid(String uuid) {
        if (uuid.length() != 32) {
            return uuid; // If format is incorrect, return as-is
        }
        return uuid.substring(0, 8) + "-" +
               uuid.substring(8, 12) + "-" +
               uuid.substring(12, 16) + "-" +
               uuid.substring(16, 20) + "-" +
               uuid.substring(20, 32);
    }

}

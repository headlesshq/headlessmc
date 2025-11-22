package io.github.headlesshq.headlessmc.launcher.command.login;

import lombok.CustomLog;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.line.CommandLine;
import io.github.headlesshq.headlessmc.auth.LoginContext;
import io.github.headlesshq.headlessmc.auth.YggdrasilClient;
import io.github.headlesshq.headlessmc.auth.YggdrasilSession;
import io.github.headlesshq.headlessmc.auth.YggdrasilAccount;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.command.AbstractLauncherCommand;

import java.io.IOException;

@CustomLog
public class YggdrasilLoginCommand extends AbstractLauncherCommand {
    private static final String DEFAULT_SERVER = "https://littleskin.cn/api/yggdrasil";

    public YggdrasilLoginCommand(Launcher ctx) {
        super(ctx, "yggdrasil", "Login with Yggdrasil authentication server (e.g. LittleSkin)");
        args.put("login <username> [password] [--server <url>]", "Login with Yggdrasil account");
        args.put("--server <url>", "Specify Yggdrasil server URL (default: " + DEFAULT_SERVER + ")");
    }

    @Override
    public void execute(String line, String... args) throws CommandException {
        // args[0] 是 "yggdrasil"，需要检查 args[1] 是否是 "login"
        if (args.length < 2 || !args[1].equalsIgnoreCase("login")) {
            ctx.log("Usage: yggdrasil login <username> [password] [--server <url>]");
            ctx.log("Example: yggdrasil login myuser mypass");
            ctx.log("Example: yggdrasil login myuser mypass --server https://littlesk.in/api/yggdrasil");
            return;
        }

        String username = null;
        String password = null;
        String serverUrl = DEFAULT_SERVER;

        // 解析参数（从 args[2] 开始，因为 args[0]="yggdrasil", args[1]="login"）
        for (int i = 2; i < args.length; i++) {
            if (args[i].equals("--server") || args[i].equals("-s")) {
                if (i + 1 < args.length) {
                    serverUrl = args[i + 1];
                    i++; // 跳过 URL
                }
            } else if (username == null) {
                username = args[i];
            } else if (password == null) {
                password = args[i];
            }
        }

        if (username == null || username.isEmpty()) {
            throw new CommandException("Username is required");
        }

        // 如果提供了密码，直接登录
        if (password != null && !password.isEmpty()) {
            try {
                login(username, password, serverUrl);
            } catch (Exception e) {
                throw new CommandException("Failed to login: " + e.getMessage(), e);
            }
        } else {
            // 如果没有提供密码，使用交互式输入
            loginWithPasswordPrompt(username, serverUrl);
        }
    }

    private void loginWithPasswordPrompt(String username, String serverUrl) {
        CommandLine clm = ctx.getCommandLine();
        String helpMessage = "Enter your password or type 'abort' to cancel the login process."
            + (clm.isHidingPasswordsSupported()
                ? ""
                : " (Your password will be visible when you type!)");
        ctx.log(helpMessage);

        boolean passwordsHiddenBefore = clm.isHidingPasswords();
        clm.setHidingPasswords(true);
        clm.setWaitingForInput(true);
        clm.setCommandContext(
            new LoginContext(ctx, clm.getCommandContext(), helpMessage) {
                @Override
                protected void onCommand(String password) {
                    try {
                        login(username, password, serverUrl);
                    } catch (Exception e) {
                        ctx.log("Failed to login: " + e.getMessage());
                    } finally {
                        returnToPreviousContext();
                        if (!passwordsHiddenBefore) {
                            clm.setHidingPasswords(false);
                        }
                        clm.setWaitingForInput(false);
                    }
                }
            });
    }

    private void login(String username, String password, String serverUrl) throws CommandException {
        logInfo("========================================");
        logInfo("开始手动登录");
        logInfo("登录服务器: " + serverUrl);
        logInfo("用户名: " + username);
        logInfo("正在验证用户名和密码...");
        
        try {
            // 创建 Yggdrasil 客户端并认证
            YggdrasilClient client = new YggdrasilClient(serverUrl);
            YggdrasilSession session = client.authenticate(username, password);
            
            logInfo("用户名密码验证正确");
            
            // 处理多角色选择
            YggdrasilSession.Profile selectedProfile = session.getSelectedProfile();
            
            // 记录找到的角色
            if (session.getAvailableProfiles() != null && !session.getAvailableProfiles().isEmpty()) {
                int profileCount = session.getAvailableProfiles().size();
                logInfo("在外置服务器找到 " + profileCount + " 个角色:");
                
                int index = 1;
                for (YggdrasilSession.Profile profile : session.getAvailableProfiles()) {
                    String marker = profile.equals(selectedProfile) ? " [当前默认]" : "";
                    logInfo("  " + index + ". " + profile.getName() + " (UUID: " + profile.getId() + ")" + marker);
                    index++;
                }
            }
            
            // 如果有多个角色，让用户选择
            if (session.getAvailableProfiles() != null && session.getAvailableProfiles().size() > 1) {
                selectProfileAndLogin(session, selectedProfile, serverUrl);
                return; // 选择在回调中完成，直接返回
            }
            
            // 只有一个角色，直接登录
            logInfo("玩家已选择: " + selectedProfile.getName());
            createAndAddAccount(serverUrl, session, selectedProfile);
            
        } catch (IOException e) {
            logError("登录失败: " + e.getMessage());
            if (log != null) {
                log.error("Yggdrasil login error", e);
            }
            throw new CommandException("Authentication failed: " + e.getMessage(), e);
        } catch (Exception e) {
            logError("登录过程发生异常: " + e.getMessage());
            if (log != null) {
                log.error("Unexpected error during login", e);
            }
            throw new CommandException("Login failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * 同时输出到控制台和日志文件
     */
    private void logInfo(String message) {
        ctx.log(message);
        if (log != null) {
            log.info(message);
        }
    }
    
    /**
     * 同时输出到控制台和日志文件（错误级别）
     */
    private void logError(String message) {
        ctx.log(message);
        if (log != null) {
            log.error(message);
        }
    }
    
    /**
     * 同时输出到控制台和日志文件（警告级别）
     */
    private void logWarn(String message) {
        ctx.log(message);
        if (log != null) {
            log.warn(message);
        }
    }

    private void selectProfileAndLogin(YggdrasilSession session, YggdrasilSession.Profile defaultProfile, String serverUrl) {
        String helpMessage = "请输入要使用的角色编号 (1-" + session.getAvailableProfiles().size() + ", 或按 Enter 使用当前角色, 输入 'abort' 取消):";
        logInfo(helpMessage);
        
        CommandLine clm = ctx.getCommandLine();
        clm.setWaitingForInput(true);
        clm.setCommandContext(
            new LoginContext(ctx, clm.getCommandContext(), helpMessage) {
                @Override
                protected void onCommand(String input) {
                    try {
                        YggdrasilSession.Profile selected = defaultProfile;
                        
                        if (input != null && !input.trim().isEmpty()) {
                            try {
                                int selectedNumber = Integer.parseInt(input.trim());
                                // 用户输入的是显示编号（从1开始），需要转换为索引（从0开始）
                                int selectedIndex = selectedNumber - 1;
                                if (selectedIndex >= 0 && selectedIndex < session.getAvailableProfiles().size()) {
                                    selected = session.getAvailableProfiles().get(selectedIndex);
                                    logInfo("玩家已选择: " + selected.getName());
                                } else {
                                    logWarn("无效的编号 " + selectedNumber + "，使用当前角色: " + defaultProfile.getName());
                                    selected = defaultProfile;
                                }
                            } catch (NumberFormatException e) {
                                logWarn("无效的输入，使用当前角色: " + defaultProfile.getName());
                                selected = defaultProfile;
                            }
                        } else {
                            logInfo("玩家已选择: " + defaultProfile.getName() + " (使用默认角色)");
                            selected = defaultProfile;
                        }
                        
                        // 创建账户并添加
                        createAndAddAccount(serverUrl, session, selected);
                    } finally {
                        returnToPreviousContext();
                        clm.setWaitingForInput(false);
                    }
                }
            });
    }

    private void createAndAddAccount(String serverUrl, YggdrasilSession session, YggdrasilSession.Profile profile) {
        String accessToken = session.getAccessToken();
        String maskedToken = maskToken(accessToken);
        
        logInfo("请求到的 accessToken: " + maskedToken);
        
        YggdrasilAccount account = new YggdrasilAccount(
            serverUrl,
            accessToken,
            session.getClientToken(),
            profile.getId(),
            profile.getName()
        );
        
        ctx.getAccountManager().addYggdrasilAccount(account);
        logInfo("登录成功！账户: " + account.getName());
        logInfo("========================================");
    }
    
    /**
     * 对 token 进行打码处理，只显示前10位和后10位，中间用*代替
     * @param token 原始 token
     * @return 打码后的 token
     */
    private String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return "null";
        }
        
        int length = token.length();
        if (length <= 20) {
            // 如果 token 长度小于等于20，全部用*代替
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) {
                sb.append("*");
            }
            return sb.toString();
        }
        
        // 前10位 + 中间* + 后10位
        String prefix = token.substring(0, 10);
        String suffix = token.substring(length - 10);
        int middleLength = length - 20;
        StringBuilder middle = new StringBuilder();
        for (int i = 0; i < middleLength; i++) {
            middle.append("*");
        }
        
        return prefix + middle.toString() + suffix;
    }
}


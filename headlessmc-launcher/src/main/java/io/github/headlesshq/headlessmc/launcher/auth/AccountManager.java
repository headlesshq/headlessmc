package io.github.headlesshq.headlessmc.launcher.auth;

import lombok.*;
import io.github.headlesshq.headlessmc.api.config.Config;
import io.github.headlesshq.headlessmc.auth.AccountJsonLoader;
import io.github.headlesshq.headlessmc.auth.ValidatedAccount;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.step.msa.StepCredentialsMsaCode;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@CustomLog
@RequiredArgsConstructor
public class AccountManager {
    private static final String OFFLINE_UUID = "22689332a7fd41919600b0fe1135ee34";

    private final List<ValidatedAccount> accounts = new ArrayList<>();
    private final List<io.github.headlesshq.headlessmc.auth.YggdrasilAccount> yggdrasilAccounts = new ArrayList<>();
    private final AccountValidator accountValidator;
    private final OfflineChecker offlineChecker;
    private final AccountStore accountStore;

    @Synchronized
    public @Nullable ValidatedAccount getPrimaryAccount() {
        return accounts.isEmpty() ? null : accounts.get(0);
    }

    @Synchronized
    public void addAccount(ValidatedAccount account) {
        removeAccount(account);
        accounts.add(0, account);
        save();
    }

    @Synchronized
    public void addYggdrasilAccount(io.github.headlesshq.headlessmc.auth.YggdrasilAccount account) {
        log.info("Adding Yggdrasil account to account manager: " + account.getName() + " / 添加 Yggdrasil 账户到账户管理器: " + account.getName());
        removeYggdrasilAccount(account);
        yggdrasilAccounts.add(0, account);
        save();
        log.info("Yggdrasil account saved: " + account.getName() + " / Yggdrasil 账户已保存: " + account.getName());
    }

    @Synchronized
    public void removeAccount(ValidatedAccount account) {
        accounts.remove(account);
        accounts.removeIf(s -> Objects.equals(account.getName(), s.getName()));
        save();
    }

    @Synchronized
    public void removeYggdrasilAccount(io.github.headlesshq.headlessmc.auth.YggdrasilAccount account) {
        yggdrasilAccounts.remove(account);
        yggdrasilAccounts.removeIf(s -> Objects.equals(account.getName(), s.getName()));
        save();
    }

    @Synchronized
    public ValidatedAccount refreshAccount(ValidatedAccount account, @Nullable Config config) throws AuthException {
        try {
            log.debug("Refreshing account " + account);
            HttpClient httpClient = MinecraftAuth.createHttpClient();
            StepFullJavaSession.FullJavaSession refreshedSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.refresh(httpClient, account.getSession());
            ValidatedAccount refreshedAccount = new ValidatedAccount(refreshedSession, account.getXuid());
            log.debug("Refreshed account: " + refreshedAccount);
            removeAccount(account);
            addAccount(refreshedAccount);
            return refreshedAccount;
        } catch (Exception e) {
            if (config != null && config.get(LauncherProperties.REFRESH_FAILURE_DELETE, false)) {
                removeAccount(account);
            }

            throw new AuthException(e.getMessage(), e);
        }
    }

    @Deprecated
    @Synchronized
    public ValidatedAccount refreshAccount(ValidatedAccount account) throws AuthException {
        return refreshAccount(account, null);
    }

    @Synchronized
    public void load(Config config) throws AuthException {
        try {
            AccountJsonLoader.AccountLoadResult result = accountStore.loadMixed();
            this.accounts.clear();
            this.accounts.addAll(result.msaAccounts);
            this.yggdrasilAccounts.clear();
            this.yggdrasilAccounts.addAll(result.yggdrasilAccounts);
        } catch (IOException e) {
            throw new AuthException(e.getMessage());
        }

        String email = config.get(LauncherProperties.EMAIL);
        String password = config.get(LauncherProperties.PASSWORD);
        if (email != null && password != null) {
            log.info("Logging in with Email and password...");
            try {
                HttpClient httpClient = MinecraftAuth.createHttpClient();
                StepFullJavaSession.FullJavaSession session = MinecraftAuth.JAVA_CREDENTIALS_LOGIN.getFromInput(
                    httpClient, new StepCredentialsMsaCode.MsaCredentials(email, password));
                ValidatedAccount validatedAccount = accountValidator.validate(session);
                addAccount(validatedAccount);
            } catch (Exception e) {
                throw new AuthException(e.getMessage(), e);
            }
        }

        if (config.get(LauncherProperties.REFRESH_ON_LAUNCH, false)) {
            ValidatedAccount primary = getPrimaryAccount();
            if (primary != null) {
                try {
                    refreshAccount(primary, config);
                } catch (AuthException e) {
                    log.error("Failed to refresh account " + primary.getName(), e);
                }
            }
        }
    }

    private void save() {
        try {
            accountStore.saveMixed(accounts, yggdrasilAccounts);
        } catch (IOException e) {
            log.error(e);
        }
    }

    @Synchronized
    public @Nullable io.github.headlesshq.headlessmc.auth.YggdrasilAccount getPrimaryYggdrasilAccount() {
        return yggdrasilAccounts.isEmpty() ? null : yggdrasilAccounts.get(0);
    }

    /**
     * Validate if Yggdrasil account token is valid
     * 验证 Yggdrasil 账户的 token 是否有效
     * @param account Yggdrasil account / Yggdrasil 账户
     * @return true if token is valid, false if invalid / true 如果 token 有效，false 如果失效
     */
    public boolean validateYggdrasilToken(io.github.headlesshq.headlessmc.auth.YggdrasilAccount account) {
        log.info("Validating Yggdrasil account token: " + account.getName() + " / 正在验证 Yggdrasil 账户 token: " + account.getName());
        log.info("Validation server: " + account.getServerUrl() + " / 验证服务器: " + account.getServerUrl());
        log.info("Validation method: Send POST request to " + account.getServerUrl() + "/authserver/validate / 验证方法: 向 " + account.getServerUrl() + "/authserver/validate 发送 POST 请求");
        log.info("Request parameters: accessToken=" + (account.getAccessToken() != null ? account.getAccessToken().substring(0, Math.min(10, account.getAccessToken().length())) + "..." : "null") + " / 请求参数: accessToken=" + (account.getAccessToken() != null ? account.getAccessToken().substring(0, Math.min(10, account.getAccessToken().length())) + "..." : "null"));
        
        try {
            io.github.headlesshq.headlessmc.auth.YggdrasilClient client = 
                new io.github.headlesshq.headlessmc.auth.YggdrasilClient(account.getServerUrl());
            boolean isValid = client.validate(account.getAccessToken(), account.getClientToken());
            
            if (isValid) {
                log.info("Token validation successful: Server returned 204 No Content, indicating token is valid / Token 验证成功: 服务器返回 204 No Content，表示 token 有效");
            } else {
                log.warn("Token validation failed: Server returned non-204 status code, indicating token is invalid or expired / Token 验证失败: 服务器返回非 204 状态码，表示 token 已失效或无效");
                log.warn("Failure reason: Server rejected validation request, possible reasons include: / 失效原因: 服务器拒绝了验证请求，可能的原因包括：");
                log.warn("  1. Token expired / Token 已过期");
                log.warn("  2. Token revoked / Token 已被撤销");
                log.warn("  3. Token format incorrect / Token 格式不正确");
                log.warn("  4. Server connection failed / 服务器连接失败");
            }
            
            return isValid;
        } catch (Exception e) {
            log.error("Exception occurred during token validation: " + e.getClass().getSimpleName() + " - " + e.getMessage() + " / Token 验证过程发生异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            log.error("Validation failure reason: " + e.getMessage() + " / 验证失败原因: " + e.getMessage());
            if (e.getCause() != null) {
                log.error("Root cause: " + e.getCause().getMessage() + " / 根本原因: " + e.getCause().getMessage());
            }
            log.warn("Token considered invalid due to validation exception / 由于验证过程异常，判定 token 失效");
            return false;
        }
    }

    public LaunchAccount getOfflineAccount(Config config) throws AuthException {
        return new LaunchAccount(
            config.get(LauncherProperties.OFFLINE_TYPE, "msa"),
            config.get(LauncherProperties.OFFLINE_USERNAME, "Offline"),
            config.get(LauncherProperties.OFFLINE_UUID, OFFLINE_UUID),
            config.get(LauncherProperties.OFFLINE_TOKEN, ""),
            config.get(LauncherProperties.XUID, ""));
    }

}

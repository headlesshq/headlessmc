package io.github.headlesshq.headlessmc.launcher.auth;

import io.github.headlesshq.headlessmc.api.settings.Config;
import io.github.headlesshq.headlessmc.auth.ValidatedAccount;
import io.github.headlesshq.headlessmc.launcher.settings.LauncherSettings;
import io.github.headlesshq.headlessmc.launcher.settings.OfflineSettings;
import jakarta.inject.Inject;
import lombok.CustomLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
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
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class AccountManager {
    private final List<ValidatedAccount> accounts = new ArrayList<>();
    private final AccountValidator accountValidator;
    private final OfflineChecker offlineChecker;
    private final AccountStore accountStore;
    private final OfflineSettings offlineSettings;
    private final LauncherSettings settings;

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
    public void removeAccount(ValidatedAccount account) {
        accounts.remove(account);
        accounts.removeIf(s -> Objects.equals(account.getName(), s.getName()));
        save();
    }

    @Synchronized
    public ValidatedAccount refreshAccount(ValidatedAccount account) throws AuthException {
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
            removeAccount(account);
            throw new AuthException(e.getMessage(), e);
        }
    }

    @Synchronized
    public void load(Config config) throws AuthException {
        try {
            List<ValidatedAccount> accounts = accountStore.load();
            this.accounts.clear();
            this.accounts.addAll(accounts);
        } catch (IOException e) {
            throw new AuthException(e.getMessage());
        }

        String email = config.get(settings.email());
        String password = config.get(settings.password());
        if (email != null) {
            if (password != null) {
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
            } else {
                throw new AuthException(settings.email().getName() + " specified, but not " + settings.password().getName());
            }
        } else if (password != null) {
            throw new AuthException(settings.password().getName() + " specified, but not " + settings.email().getName());
        }

        if (config.get(settings.refreshOnLaunch())) {
            ValidatedAccount primary = getPrimaryAccount();
            if (primary != null) {
                try {
                    refreshAccount(primary);
                } catch (AuthException e) {
                    log.error("Failed to refresh account " + primary.getName(), e);
                }
            }
        }
    }

    private void save() {
        try {
            accountStore.save(accounts);
        } catch (IOException e) {
            log.error(e);
        }
    }

    public LaunchAccount getOfflineAccount(Config config) throws AuthException {
        return new LaunchAccount(
            config.get(offlineSettings.offlineType()),
            config.get(offlineSettings.offlineUserName()),
            config.get(offlineSettings.offlineUUID()),
            config.get(offlineSettings.offlineToken()),
            config.get(settings.xuid()));
    }

}

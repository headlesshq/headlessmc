package io.github.headlesshq.headlessmc.launcher.auth;

import io.github.headlesshq.headlessmc.api.settings.Config;
import io.github.headlesshq.headlessmc.auth.AccountJsonLoader;
import io.github.headlesshq.headlessmc.auth.ValidatedAccount;
import io.github.headlesshq.headlessmc.launcher.settings.LauncherSettings;
import jakarta.inject.Inject;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@CustomLog
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class AccountStore {
    private final AccountJsonLoader accountJsonLoader;
    private final LauncherSettings settings;
    private final Config config;

    public void save(List<ValidatedAccount> accounts) throws IOException {
        if (!config.get(settings.storeAccounts())) {
            return;
        }

        Path path = config.getApplicationPath().resolve("auth").resolve("accounts.json");
        accountJsonLoader.save(path, accounts);
    }

    public List<ValidatedAccount> load() throws IOException {
        Path path = config.getApplicationPath().resolve("auth").resolve("accounts.json");
        return accountJsonLoader.load(path);
    }

}

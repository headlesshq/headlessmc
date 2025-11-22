package io.github.headlesshq.headlessmc.launcher.auth;

import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import io.github.headlesshq.headlessmc.auth.AccountJsonLoader;
import io.github.headlesshq.headlessmc.auth.ValidatedAccount;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.launcher.files.LauncherConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@CustomLog
@RequiredArgsConstructor
public class AccountStore {
    private final AccountJsonLoader accountJsonLoader;
    private final LauncherConfig launcherConfig;

    public AccountStore(LauncherConfig launcherConfig) {
        this(new AccountJsonLoader(), launcherConfig);
    }

    public void save(List<ValidatedAccount> accounts) throws IOException {
        saveMixed(accounts, new ArrayList<>());
    }

    public void saveMixed(List<ValidatedAccount> msaAccounts, List<io.github.headlesshq.headlessmc.auth.YggdrasilAccount> yggdrasilAccounts) throws IOException {
        if (!launcherConfig.getConfig().getConfig().get(LauncherProperties.STORE_ACCOUNTS, true)) {
            return;
        }

        File file = launcherConfig.getFileManager().create("auth", ".accounts.json");
        accountJsonLoader.saveMixed(file.toPath(), msaAccounts, yggdrasilAccounts);
    }

    public List<ValidatedAccount> load() throws IOException {
        AccountJsonLoader.AccountLoadResult result = loadMixed();
        return result.msaAccounts;
    }

    public AccountJsonLoader.AccountLoadResult loadMixed() throws IOException {
        File file = launcherConfig.getFileManager().create("auth", ".accounts.json");
        return accountJsonLoader.loadMixed(file.toPath());
    }

}

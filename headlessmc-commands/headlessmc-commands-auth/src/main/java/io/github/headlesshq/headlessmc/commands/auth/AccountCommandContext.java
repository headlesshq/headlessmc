package io.github.headlesshq.headlessmc.commands.auth;

import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.auth.AuthProvider;
import io.github.headlesshq.headlessmc.auth.LastUsedAccountService;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface AccountCommandContext {
    LastUsedAccountService getLastUsedAccountService();

    TableProvider getTableProvider();

    AuthProvider getAuthProvider();

    Console getConsole();

    List<AuthProvider> getAuthProviders();

    default Account getAccount(@Nullable String parameter, String action) {
        if (parameter == null) {
            throw new IllegalArgumentException("You need to specify the name of the account to " + action + ".");
        }

        AuthProvider authProvider = getAuthProvider();
        return authProvider.getAccount(parameter)
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Failed to find account with name %s on %s provider", parameter, authProvider.getName())
            ));
    }

}

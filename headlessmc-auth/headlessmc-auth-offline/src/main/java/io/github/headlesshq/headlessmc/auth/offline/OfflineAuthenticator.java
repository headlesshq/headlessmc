package io.github.headlesshq.headlessmc.auth.offline;

import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.auth.AuthException;
import io.github.headlesshq.headlessmc.auth.AuthStoreException;
import io.github.headlesshq.headlessmc.auth.Authenticator;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.Password;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Unmodifiable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
final class OfflineAuthenticator implements Authenticator {
    private final OfflineAuthProvider provider;

    @Override
    public @Unmodifiable Map<String, Method<Console>> getMethods() {
        Map<String, Method<Console>> result = new LinkedHashMap<>();
        result.put(Authenticator.DEFAULT_METHOD, loginWithUserInput());
        return result;
    }

    private Method<Console> loginWithUserInput() throws AuthException {
        return console -> {
            String name = console.read("Enter the account name:");
            String uuid = console.read("Enter the account uuid (optional):");
            if (uuid.trim().isEmpty()) {
                uuid = UUID.randomUUID().toString();
            }

            String token;
            try (Password password = console.readPassword("Enter the account token (optional, hidden):")) {
                char[] pwdChars = password.get();
                token = new String(pwdChars);
            }

            String type = console.read("Enter the account type (default: " + Account.TYPE_MSA + "):");
            if (type.trim().isEmpty()) {
                type = Account.TYPE_MSA;
            }

            Account account = new Account(provider.getName(), name, uuid, token, type, "");
            try {
                provider.getAuthStore().add(account.getName(), account);
            } catch (AuthStoreException e) {
                provider.getExceptionLogger().accept(e);
            }

            return account;
        };
    }

}

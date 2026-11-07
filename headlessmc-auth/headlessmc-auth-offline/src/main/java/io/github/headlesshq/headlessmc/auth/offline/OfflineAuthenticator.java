package io.github.headlesshq.headlessmc.auth.offline;

import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.auth.AuthException;
import io.github.headlesshq.headlessmc.auth.AuthStoreException;
import io.github.headlesshq.headlessmc.auth.Authenticator;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.prompt.Answers;
import io.github.headlesshq.headlessmc.console.prompt.Form;
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
            Form form = Form.builder("Create an offline account")
                .text("name", "Account name")
                .optionalText("uuid", "Account uuid, leave empty for a random one", null)
                .optionalSecret("token", "Account token")
                .optionalText("type", "Account type", Account.TYPE_MSA)
                .build();

            String name;
            String uuid;
            String token;
            String type;
            try (Answers answers = console.ask(form)) {
                name = answers.getString("name").trim();
                uuid = answers.getString("uuid").trim();
                token = new String(answers.getPassword("token").get());
                type = answers.getString("type").trim();
            }

            if (uuid.isEmpty()) {
                uuid = UUID.randomUUID().toString();
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

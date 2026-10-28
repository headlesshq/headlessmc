package io.github.headlesshq.headlessmc.auth;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.headlesshq.headlessmc.auth.FakeAuthProvider.account;
import static org.junit.jupiter.api.Assertions.*;

public class AccountSecretSourceTest {
    @Test
    public void suppliesTheTokensOfTheAccountsInMemoryOfAllProviders() {
        FakeAuthProvider msa = new FakeAuthProvider("msa", withToken(account("msa", "Alex"), "stored-token"));
        msa.loaded.add(withToken(account("msa", "Alex"), "msa-token"));
        FakeAuthProvider disabled = new FakeAuthProvider("other");
        disabled.loaded.add(withToken(account("other", "Steve"), "other-token"));
        disabled.enabled = false;
        AccountSecretSource source = new AccountSecretSource(new AuthServiceImpl(List.of(msa, disabled), msa));

        assertEquals(List.of("msa-token", "other-token"), List.copyOf(source.getSecrets()));

        msa.loaded.add(withToken(account("msa", "Notch"), "new-token"));

        assertEquals(List.of("msa-token", "new-token", "other-token"), List.copyOf(source.getSecrets()));
    }

    private static Account withToken(Account account, String token) {
        return new Account(
            account.getProvider(), account.getName(), account.getUuid(), token, account.getType(), account.getXuid()
        );
    }

}

package io.github.headlesshq.headlessmc.auth;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static io.github.headlesshq.headlessmc.auth.FakeAuthProvider.account;
import static org.junit.jupiter.api.Assertions.*;

public class AnyAuthProviderTest {
    private final FakeAuthProvider offline = new FakeAuthProvider("offline", account("offline", "Steve"));
    private final FakeAuthProvider msa = new FakeAuthProvider("msa", account("msa", "Alex"));
    private final AnyAuthProvider any = new AnyAuthProvider(new AuthServiceImpl(List.of(offline, msa), msa));

    @Test
    public void findsUniqueAccountAcrossProviders() {
        Optional<Account> account = any.getAccount("Alex");
        assertTrue(account.isPresent());
        assertEquals("msa", account.get().getProvider());
    }

    @Test
    public void unknownAccountIsEmpty() {
        assertTrue(any.getAccount("Herobrine").isEmpty());
    }

    @Test
    public void ambiguousAccountNameThrows() {
        offline.accounts.add(account("offline", "Alex"));

        AuthException e = assertThrows(AuthException.class, () -> any.getAccount("Alex"));
        assertTrue(e.getMessage().contains("offline"));
        assertTrue(e.getMessage().contains("msa"));
    }

    @Test
    public void aggregatesAllAccounts() {
        assertEquals(2, any.getAccounts().size());
    }

    @Test
    public void removesAccountViaItsProvider() {
        Account alex = msa.accounts.getFirst();
        any.removeAccount(alex);
        assertTrue(msa.accounts.isEmpty());
        assertEquals(1, offline.accounts.size());
    }

    @Test
    public void removingAccountOfUnknownProviderThrows() {
        assertThrows(AuthException.class, () -> any.removeAccount(account("unknown", "Steve")));
    }

    @Test
    public void refreshAndAuthenticatorDelegate() {
        Account alex = account("msa", "Alex");
        assertEquals(alex, any.refresh(alex));
        assertNotNull(any.getAuthenticator());
        assertEquals("any", any.getName());
        assertEquals(0, any.getVersion());
    }

}

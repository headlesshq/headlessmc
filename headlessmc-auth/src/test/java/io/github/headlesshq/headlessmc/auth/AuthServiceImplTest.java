package io.github.headlesshq.headlessmc.auth;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.github.headlesshq.headlessmc.auth.FakeAuthProvider.account;
import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceImplTest {
    private final FakeAuthProvider offline = new FakeAuthProvider("offline", account("offline", "Steve"));
    private final FakeAuthProvider msa = new FakeAuthProvider("msa", account("msa", "Alex"));
    private final AuthServiceImpl service = new AuthServiceImpl(List.of(offline, msa), msa);

    @Test
    public void refreshDelegatesToMatchingProvider() {
        Account account = account("msa", "Alex");
        assertEquals(account, service.refresh(account));
        assertEquals(account, msa.refreshed);
        assertNull(offline.refreshed);
    }

    @Test
    public void refreshThrowsForUnknownProvider() {
        AuthException e = assertThrows(AuthException.class, () -> service.refresh(account("unknown", "Steve")));
        assertTrue(e.getMessage().contains("offline,msa"));
    }

    @Test
    public void aggregatesAccounts() {
        assertEquals(2, service.getAccounts().size());
    }

    @Test
    public void findsProviderCaseInsensitively() {
        assertEquals(Optional.of(msa), service.getProvider("MSA"));
        assertTrue(service.getProvider("unknown").isEmpty());
    }

    @Test
    public void inMemoryStoreIsCaseInsensitiveAndSorted() {
        InMemoryAuthStore<String> store = new InMemoryAuthStore<>();
        store.add("Bob", "b");
        store.add("alice", "a");

        assertEquals(Optional.of("b"), store.getById("bob"));
        assertEquals(List.of("a", "b"), List.copyOf(store.read().values()));
        assertEquals(List.of("a", "b"), store.stream().toList());

        store.remove("ALICE");
        assertTrue(store.getById("alice").isEmpty());

        store.save(Map.of("carol", "c"));
        assertEquals(Optional.of("c"), store.getById("Carol"));
        assertEquals(1, store.read().size());
    }

    @Test
    public void accountsCompareByNameIgnoringCase() {
        assertTrue(account("p", "alice").compareTo(account("p", "BOB")) < 0);
        assertEquals(0, account("p", "Steve").compareTo(account("other", "steve")));
    }

    @Test
    public void credentialsToStringHidesPassword() {
        Credentials credentials = new Credentials("mail@example.com", "secret");
        assertFalse(credentials.toString().contains("secret"));
        assertTrue(credentials.toString().contains("mail@example.com"));
    }

}

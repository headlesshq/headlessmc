package io.github.headlesshq.headlessmc.auth;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.github.headlesshq.headlessmc.auth.FakeAuthProvider.account;
import static org.junit.jupiter.api.Assertions.*;

public class LastUsedAccountServiceImplTest {
    private final FakeAuthProvider offline = new FakeAuthProvider("offline", account("offline", "Steve"));
    private final FakeAuthProvider msa = new FakeAuthProvider("msa", account("msa", "Alex"));
    private final AuthServiceImpl authService = new AuthServiceImpl(List.of(offline, msa), msa);
    private final InMemoryAuthStore<List<AccountInfo>> store = new InMemoryAuthStore<>();
    private final List<String> loggedErrors = new ArrayList<>();
    private final LastUsedAccountServiceImpl service = new LastUsedAccountServiceImpl(
        store, authService, (message, e) -> loggedErrors.add(message));

    @Test
    public void emptyStoreHasNoLastUsedAccount() {
        assertTrue(service.getLastUsedAccount().isEmpty());
    }

    @Test
    public void returnsMostRecentlyUsedAccount() {
        service.setLastUsedAccount(account("offline", "Steve"));
        service.setLastUsedAccount(account("msa", "Alex"));

        Optional<Account> last = service.getLastUsedAccount();

        assertTrue(last.isPresent());
        assertEquals("Alex", last.get().getName());
    }

    @Test
    public void settingSameAccountAgainMovesItToFront() {
        service.setLastUsedAccount(account("msa", "Alex"));
        service.setLastUsedAccount(account("offline", "Steve"));
        service.setLastUsedAccount(account("msa", "Alex"));

        assertEquals(2, store.getById("latest").orElseThrow().size());
        assertEquals("Alex", service.getLastUsedAccount().orElseThrow().getName());
    }

    @Test
    public void fallsBackWhenAccountNoLongerExists() {
        service.setLastUsedAccount(account("offline", "Steve"));
        service.setLastUsedAccount(account("msa", "Gone"));

        assertEquals("Steve", service.getLastUsedAccount().orElseThrow().getName());
    }

    @Test
    public void prunesEntriesOfFailingProviders() {
        service.setLastUsedAccount(account("offline", "Steve"));
        service.setLastUsedAccount(account("msa", "Alex"));
        msa.failOnGetAccount = true;

        Optional<Account> last = service.getLastUsedAccount();

        assertEquals("Steve", last.orElseThrow().getName());
        assertEquals(1, loggedErrors.size());
        assertEquals(1, store.getById("latest").orElseThrow().size(), "failing entry should be pruned");
    }

}

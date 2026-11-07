package io.github.headlesshq.headlessmc.auth.offline;

import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.auth.AuthException;
import io.github.headlesshq.headlessmc.auth.AuthStore;
import io.github.headlesshq.headlessmc.auth.AuthStoreException;
import io.github.headlesshq.headlessmc.auth.Authenticator;
import io.github.headlesshq.headlessmc.auth.InMemoryAuthStore;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.ConsoleException;
import io.github.headlesshq.headlessmc.console.Password;
import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.console.prompt.Answers;
import io.github.headlesshq.headlessmc.console.prompt.Form;
import io.github.headlesshq.headlessmc.console.prompt.Prompt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OfflineAuthProviderTest {
    /** An {@link AuthStore} whose every operation fails. */
    private static final class BrokenStore implements AuthStore<Account> {
        @Override
        public SortedMap<String, Account> read() {
            throw new AuthStoreException("broken");
        }

        @Override
        public void add(String id, Account value) {
            throw new AuthStoreException("broken");
        }

        @Override
        public void save(Map<String, Account> values) {
            throw new AuthStoreException("broken");
        }

        @Override
        public void remove(String id) {
            throw new AuthStoreException("broken");
        }

        @Override
        public Optional<Account> getById(String id) {
            throw new AuthStoreException("broken");
        }

        @Override
        public Collection<Account> loaded() {
            return Collections.emptyList();
        }
    }

    private final List<Throwable> logged = new ArrayList<>();

    private AuthStore<Account> store;
    private OfflineAuthProvider provider;

    @BeforeEach
    void setup() {
        store = new InMemoryAuthStore<>();
        provider = new OfflineAuthProvider(store, logged::add, "offline", OfflineAuthProvider.VERSION);
    }

    private Account account(String name) {
        return new Account("offline", name, UUID.randomUUID().toString(), "token", Account.TYPE_MSA, "");
    }

    private Authenticator.Method<Console> defaultMethod() {
        return provider.getAuthenticator().getMethods().get(Authenticator.DEFAULT_METHOD);
    }

    @Test
    void exposesNameAndVersion() {
        assertEquals("offline", provider.getName());
        assertEquals(Account.SCHEMA_VERSION, provider.getVersion());
    }

    @Test
    void refreshReturnsTheSameAccount() {
        Account account = account("Steve");
        assertSame(account, provider.refresh(account));
    }

    @Test
    void refreshRejectsAccountsOfOtherProviders() {
        Account foreign = new Account("msa", "Steve", "uuid", "token", Account.TYPE_MSA, "");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> provider.refresh(foreign));
        assertTrue(e.getMessage().contains("Cannot refresh account"));
    }

    @Test
    void readsAccountsFromTheStore() {
        Account account = account("Steve");
        store.add("Steve", account);

        assertEquals(Optional.of(account), provider.getAccount("Steve"));
        assertEquals(List.of(account), provider.getAccounts());
    }

    @Test
    void removesAccountsFromTheStore() {
        store.add("Steve", account("Steve"));

        provider.removeAccount(account("Steve"));

        assertEquals(List.of(), provider.getAccounts());
    }

    @Test
    void storeFailuresAreLoggedInsteadOfThrown() {
        OfflineAuthProvider broken = new OfflineAuthProvider(new BrokenStore(), logged::add, "offline", 0L);

        assertEquals(Optional.empty(), broken.getAccount("Steve"));
        assertEquals(List.of(), broken.getAccounts());
        broken.removeAccount(account("Steve"));

        assertEquals(3, logged.size());
    }

    @Test
    void loginReadsTheAccountFromTheConsole() {
        RecordingConsole console = new RecordingConsole()
            .addInput("Steve")
            .addInput("0-0-0-0-1")
            .addInput("legacy");
        console.addPassword("secret");

        Account account = defaultMethod().login(console);

        assertEquals("Steve", account.getName());
        assertEquals("0-0-0-0-1", account.getUuid());
        assertEquals("secret", account.getToken());
        assertEquals("legacy", account.getType());
        assertEquals("offline", account.getProvider());
        assertEquals(Optional.of(account), provider.getAccount("Steve"));
    }

    @Test
    void loginDefaultsUuidAndType() {
        RecordingConsole console = new RecordingConsole().addInput("Steve").addInput("").addInput("");
        console.addPassword("");

        Account account = defaultMethod().login(console);

        assertEquals(Account.TYPE_MSA, account.getType());
        assertDoesNotThrow(() -> UUID.fromString(account.getUuid()));
    }

    @Test
    void loginLogsStoreFailures() {
        OfflineAuthProvider broken = new OfflineAuthProvider(new BrokenStore(), logged::add, "offline", 0L);
        RecordingConsole console = new RecordingConsole().addInput("Steve").addInput("").addInput("");
        console.addPassword("");

        Account account = broken.getAuthenticator().getMethods().get(Authenticator.DEFAULT_METHOD).login(console);

        assertEquals("Steve", account.getName());
        assertEquals(1, logged.size());
    }

    @Test
    void abortingTheLoginStoresNoAccount() {
        RecordingConsole console = new RecordingConsole().addInput("Steve").addInterrupt();

        assertThrows(ConsoleException.Interrupted.class, () -> defaultMethod().login(console));
        assertEquals(List.of(), provider.getAccounts());
    }

    @Test
    void loginAsksAllQuestionsAsOneForm() {
        List<Form> forms = new ArrayList<>();
        RecordingConsole console = new RecordingConsole() {
            @Override
            public Answers ask(Form form) {
                forms.add(form);
                // e.g. an MCP client that cannot ask for secrets skips the optional token
                return new Answers()
                    .put("name", "Steve")
                    .put("uuid", "")
                    .put("token", Password.of(""))
                    .put("type", Account.TYPE_MSA);
            }
        };

        Account account = defaultMethod().login(console);

        assertEquals("Steve", account.getName());
        assertEquals(1, forms.size());
        assertEquals(List.of("name", "uuid", "token", "type"),
            forms.get(0).getPrompts().stream().map(Prompt::getId).toList());
        assertEquals(List.of(), console.lines());
    }

    @Test
    void credentialLoginIsUnsupported() throws AuthException {
        assertEquals(Optional.empty(), provider.getAuthenticator().loginWithCredentials());
        assertEquals(Optional.empty(), provider.getAuthenticator().loginWithInteractiveCredentials());
    }

}

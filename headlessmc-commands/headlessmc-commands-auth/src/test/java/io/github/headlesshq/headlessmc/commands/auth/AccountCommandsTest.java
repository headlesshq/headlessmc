package io.github.headlesshq.headlessmc.commands.auth;

import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.auth.AuthException;
import io.github.headlesshq.headlessmc.auth.AuthProvider;
import io.github.headlesshq.headlessmc.auth.AuthService;
import io.github.headlesshq.headlessmc.auth.AuthServiceImpl;
import io.github.headlesshq.headlessmc.auth.Authenticator;
import io.github.headlesshq.headlessmc.auth.LastUsedAccountService;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.console.format.SimpleTableProvider;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AccountCommandsTest {
    /** An {@link AuthProvider} over an in-memory list, with a scripted login method. */
    private static final class TestProvider implements AuthProvider {
        private final String name;
        private final List<Account> accounts = new ArrayList<>();

        private AuthException loginFailure;

        TestProvider(String name, Account... accounts) {
            this.name = name;
            this.accounts.addAll(List.of(accounts));
        }

        @Override
        public Account refresh(Account account) {
            Account refreshed = new Account(
                account.getProvider(), account.getName(), account.getUuid(),
                "refreshed-token", account.getType(), account.getXuid()
            );
            accounts.replaceAll(existing -> existing.getName().equals(account.getName()) ? refreshed : existing);
            return refreshed;
        }

        @Override
        public Optional<Account> getAccount(String name) {
            return accounts.stream().filter(account -> account.getName().equals(name)).findFirst();
        }

        @Override
        public List<Account> getAccounts() {
            return accounts;
        }

        @Override
        public void removeAccount(Account account) {
            accounts.remove(account);
        }

        @Override
        public Authenticator getAuthenticator() {
            return () -> {
                Map<String, Authenticator.Method<Console>> methods = new LinkedHashMap<>();
                methods.put(Authenticator.DEFAULT_METHOD, console -> {
                    if (loginFailure != null) {
                        throw loginFailure;
                    }

                    return account(name, "LoggedIn");
                });
                return methods;
            };
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getVersion() {
            return 0;
        }
    }

    private static Account account(String provider, String name) {
        return new Account(provider, name, "uuid-" + name, "token", Account.TYPE_MSA, "xuid");
    }

    private final RecordingConsole console = new RecordingConsole();
    private final TableProvider tables = new SimpleTableProvider();

    private final List<Account> lastUsed = new ArrayList<>();
    private final LastUsedAccountService lastUsedAccountService = new LastUsedAccountService() {
        @Override
        public Optional<Account> getLastUsedAccount() {
            return lastUsed.isEmpty() ? Optional.empty() : Optional.of(lastUsed.getLast());
        }

        @Override
        public void setLastUsedAccount(Account account) {
            lastUsed.add(account);
        }
    };

    private TestProvider offline;
    private TestProvider msa;
    private AuthService authService;
    private AccountCommand command;

    @BeforeEach
    void setup() {
        offline = new TestProvider("offline", account("offline", "Steve"));
        msa = new TestProvider("msa", account("msa", "Alex"));
        authService = new AuthServiceImpl(List.of(offline, msa), offline);
        command = new AccountCommand(lastUsedAccountService, tables, authService, console);
    }

    // ------------------------------------------------------- AccountCommand

    @Test
    void defaultProviderIsAny() {
        assertEquals("any", command.getAuthProvider().getName());
        assertEquals(List.of(offline, msa), command.getAuthProviders());
    }

    @Test
    void explicitAnyProviderBehavesLikeTheDefault() {
        command.setProvider("ANY");

        assertEquals("any", command.getAuthProvider().getName());
        assertEquals(List.of(offline, msa), command.getAuthProviders());
    }

    @Test
    void namedProviderIsResolved() {
        command.setProvider("msa");

        assertSame(msa, command.getAuthProvider());
        assertEquals(List.of(msa), command.getAuthProviders());
    }

    @Test
    void unknownProviderThrows() {
        command.setProvider("nope");

        assertThrows(IllegalArgumentException.class, command::getAuthProvider);
        assertThrows(IllegalArgumentException.class, command::getAuthProviders);
    }

    @Test
    void runningTheAccountCommandLogsIn() {
        command.setProvider("offline");
        command.run();

        assertEquals(1, lastUsed.size());
        assertTrue(console.output().contains("Logged in to account LoggedIn"));
    }

    // --------------------------------------------------- AccountCommandContext

    @Test
    void resolvesAccountsByName() {
        command.setProvider("offline");

        assertEquals("Steve", command.getAccount("Steve", "select").getName());
    }

    @Test
    void missingAccountNameThrows() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> command.getAccount(null, "select")
        );
        assertTrue(e.getMessage().contains("name of the account to select"));
    }

    @Test
    void unknownAccountThrows() {
        command.setProvider("offline");

        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> command.getAccount("nope", "remove")
        );
        assertTrue(e.getMessage().contains("Failed to find account with name nope"));
    }

    // ---------------------------------------------------------- LoginCommand

    private LoginCommand login() {
        LoginCommand login = new LoginCommand();
        login.setContext(command);
        return login;
    }

    @Test
    void loginUsesTheDefaultMethod() {
        command.setProvider("offline");
        login().run();

        assertEquals("LoggedIn", lastUsed.getFirst().getName());
    }

    @Test
    void unknownLoginMethodThrows() {
        command.setProvider("offline");
        LoginCommand login = login();
        login.setMethod("nope");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, login::run);
        assertTrue(e.getMessage().contains("Failed to find method nope"));
    }

    @Test
    void loginFailuresArePropagated() {
        offline.loginFailure = new AuthException("boom");
        command.setProvider("offline");

        AuthException e = assertThrows(AuthException.class, login()::run);
        assertEquals("boom", e.getMessage());
    }

    @Test
    void missingWebviewSupportIsReported() {
        offline.loginFailure = new AuthException(new NoClassDefFoundError("javafx"));
        command.setProvider("offline");
        LoginCommand login = login();
        login.setMethod(Authenticator.METHOD_WEBVIEW);

        // the webview method is not registered, so the lookup fails first
        assertThrows(IllegalArgumentException.class, login::run);
    }

    // ----------------------------------------------------------- ListCommand

    private ListCommand list() {
        ListCommand list = new ListCommand();
        list.setContext(command);
        return list;
    }

    @Test
    void listsAccountsOfAllProviders() {
        list().run();

        String output = console.output();
        assertTrue(output.contains("Steve"));
        assertTrue(output.contains("Alex"));
        assertTrue(output.contains("uuid-Steve"));
    }

    @Test
    void listsAccountsOfASingleProvider() {
        command.setProvider("msa");
        list().run();

        String output = console.output();
        assertTrue(output.contains("Alex"));
        assertFalse(output.contains("Steve"));
    }

    @Test
    void listsLoginMethods() {
        ListCommand list = list();
        list.setListMethods(true);
        list.run();

        String output = console.output();
        assertTrue(output.contains("offline"));
        assertTrue(output.contains(Authenticator.DEFAULT_METHOD));
    }

    // --------------------------------------------------------- RemoveCommand

    @Test
    void removesAnAccount() {
        command.setProvider("offline");
        RemoveCommand remove = new RemoveCommand();
        remove.setContext(command);
        remove.setName("Steve");
        remove.run();

        assertEquals(List.of(), offline.getAccounts());
    }

    @Test
    void removeWithoutNameThrows() {
        RemoveCommand remove = new RemoveCommand();
        remove.setContext(command);

        assertThrows(IllegalArgumentException.class, remove::run);
    }

    // --------------------------------------------------------- SelectCommand

    @Test
    void selectsAnAccount() {
        command.setProvider("offline");
        SelectCommand select = new SelectCommand();
        select.setContext(command);
        select.setName("Steve");
        select.run();

        assertEquals("Steve", lastUsed.getFirst().getName());
    }

    // -------------------------------------------------------- RefreshCommand

    @Test
    void refreshesAnAccount() {
        command.setProvider("offline");
        RefreshCommand refresh = new RefreshCommand();
        refresh.setContext(command);
        refresh.setName("Steve");
        refresh.run();

        assertEquals("refreshed-token", lastUsed.getFirst().getToken());
    }

    // ----------------------------------------------------------- completions

    @Test
    void completions() {
        assertEquals(List.of("Steve", "Alex"), toList(new AccountCompletions(authService)));
        assertEquals(List.of("offline", "msa"), toList(new AuthProviderCompletions(authService)));
    }

    private List<String> toList(Iterable<String> iterable) {
        List<String> result = new ArrayList<>();
        iterable.forEach(result::add);
        return result;
    }

}

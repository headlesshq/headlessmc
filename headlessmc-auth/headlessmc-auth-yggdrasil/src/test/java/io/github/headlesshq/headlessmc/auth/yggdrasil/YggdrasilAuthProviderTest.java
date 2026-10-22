package io.github.headlesshq.headlessmc.auth.yggdrasil;

import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.auth.AuthStore;
import io.github.headlesshq.headlessmc.auth.Authenticator;
import io.github.headlesshq.headlessmc.auth.InMemoryAuthStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class YggdrasilAuthProviderTest {
    private static final String SERVER = "https://example.com";

    private final List<Throwable> logged = new ArrayList<>();
    private AuthStore<YggdrasilAccount> store;
    private YggdrasilClientFactory clientFactory;

    @BeforeEach
    void setup() {
        store = new InMemoryAuthStore<>();
        clientFactory = new YggdrasilClientFactory();
    }

    private YggdrasilAuthProvider provider(Optional<String> url) {
        return provider(url, store);
    }

    private YggdrasilAuthProvider provider(Optional<String> url, AuthStore<YggdrasilAccount> store) {
        return new YggdrasilAuthProvider(
            store,
            clientFactory,
            () -> url,
            logged::add,
            YggdrasilAuthProvider.NAME,
            YggdrasilAuthProvider.VERSION
        );
    }

    private YggdrasilAccount account(String name) {
        return new YggdrasilAccount(
            SERVER, "access-token", "client-token",
            "0123456789abcdef0123456789abcdef", name, "login-name", "login-password");
    }

    private Account asAccount(String name) {
        return account(name).asAccount();
    }

    @Test
    void exposesNameAndVersion() {
        YggdrasilAuthProvider provider = provider(Optional.empty());

        assertEquals(YggdrasilAuthProvider.NAME, provider.getName());
        assertEquals(YggdrasilAuthProvider.VERSION, provider.getVersion());
    }

    @Test
    void isDisabledWithoutConfiguredUrl() {
        assertFalse(provider(Optional.empty()).isEnabled());
    }

    @Test
    void isDisabledWithBlankUrl() {
        assertFalse(provider(Optional.of("   ")).isEnabled());
    }

    @Test
    void isEnabledWithConfiguredUrl() {
        assertTrue(provider(Optional.of(SERVER)).isEnabled());
    }

    @Test
    void handsTheAccessTokenToTheGame() {
        Account account = asAccount("Steve");

        // the game needs the accessToken, not the clientToken
        assertEquals("access-token", account.getToken());
        assertEquals(YggdrasilAuthProvider.NAME, account.getProvider());
        assertEquals(YggdrasilAuthProvider.TYPE, account.getType());
        assertEquals("", account.getXuid());
    }

    @Test
    void readsAccountsFromTheStore() {
        YggdrasilAuthProvider provider = provider(Optional.of(SERVER));
        store.add("Steve", account("Steve"));

        assertEquals(Optional.of(asAccount("Steve")), provider.getAccount("Steve"));
        assertEquals(List.of(asAccount("Steve")), provider.getAccounts());
    }

    @Test
    void unknownAccountIsEmpty() {
        YggdrasilAuthProvider provider = provider(Optional.of(SERVER));

        assertEquals(Optional.empty(), provider.getAccount("NotThere"));
        assertEquals(List.of(), provider.getAccounts());
    }

    @Test
    void removesAccountsFromTheStore() {
        YggdrasilAuthProvider provider = provider(Optional.of(SERVER));
        store.add("Steve", account("Steve"));

        provider.removeAccount(asAccount("Steve"));

        assertEquals(List.of(), provider.getAccounts());
    }

    @Test
    void storeFailuresAreLoggedInsteadOfThrown() {
        AuthStore<YggdrasilAccount> broken = new BrokenStore();
        YggdrasilAuthProvider provider = provider(Optional.of(SERVER), broken);

        assertEquals(Optional.empty(), provider.getAccount("Steve"));
        assertEquals(List.of(), provider.getAccounts());
        provider.removeAccount(asAccount("Steve"));

        assertEquals(3, logged.size());
    }

    @Test
    void exposesTheDefaultLoginMethod() {
        Authenticator authenticator = provider(Optional.of(SERVER)).getAuthenticator();

        assertTrue(authenticator.getMethods().containsKey(Authenticator.DEFAULT_METHOD));
    }

    @Test
    void credentialsLoginIsSupported() {
        assertTrue(provider(Optional.of(SERVER)).getAuthenticator().loginWithCredentials().isPresent());
    }

    @Test
    void interactiveCredentialsAreAvailable() {
        assertTrue(provider(Optional.of(SERVER))
            .getAuthenticator()
            .loginWithInteractiveCredentials()
            .isPresent());
    }

    /** An {@link AuthStore} whose every operation fails. */
    private static final class BrokenStore implements AuthStore<YggdrasilAccount> {
        @Override
        public java.util.SortedMap<String, YggdrasilAccount> read() {
            throw new io.github.headlesshq.headlessmc.auth.AuthStoreException("broken");
        }

        @Override
        public void add(String id, YggdrasilAccount value) {
            throw new io.github.headlesshq.headlessmc.auth.AuthStoreException("broken");
        }

        @Override
        public void save(java.util.Map<String, YggdrasilAccount> values) {
            throw new io.github.headlesshq.headlessmc.auth.AuthStoreException("broken");
        }

        @Override
        public void remove(String id) {
            throw new io.github.headlesshq.headlessmc.auth.AuthStoreException("broken");
        }

        @Override
        public Optional<YggdrasilAccount> getById(String id) {
            throw new io.github.headlesshq.headlessmc.auth.AuthStoreException("broken");
        }
    }

}

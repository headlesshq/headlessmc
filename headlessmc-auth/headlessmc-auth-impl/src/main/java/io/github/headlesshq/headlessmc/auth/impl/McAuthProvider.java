package io.github.headlesshq.headlessmc.auth.impl;

import io.github.headlesshq.headlessmc.auth.*;
import io.github.headlesshq.headlessmc.auth.store.gson.GsonFileAuthStore;
import io.github.headlesshq.headlessmc.exceptions.NotFoundException;
import lombok.Getter;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.constants.StatusCodes;
import net.lenni0451.commons.httpclient.exceptions.HttpRequestException;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.java.model.MinecraftProfile;
import net.raphimc.minecraftauth.java.model.MinecraftToken;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Getter
public class McAuthProvider implements AuthProvider {
    private static final long VERSION = 0L;
    /**
     * The game uses the Minecraft token it has been launched with for as long as it runs.
     * A token is valid for 24 hours, a launch gets a new one if less than half of that is left.
     */
    private static final long MIN_TOKEN_VALIDITY_MS = TimeUnit.HOURS.toMillis(12);

    // Eventually have own fork of MinecraftAuth, with support for Java 11 HttpClient, and Jackson instead of GSON
    final JavaAuthManager.Builder authManagerBuilder;
    final AuthStore<JavaAuthManager> authStore;
    final HttpClient httpClient;
    final String name;
    final long version = VERSION;

    public static McAuthProvider of(String providerName, @Nullable String userAgent, @Nullable Path authStoreFile) {
        HttpClient httpClient = userAgent == null
            ? MinecraftAuth.createHttpClient()
            : MinecraftAuth.createHttpClient(userAgent);
        AuthStore<JavaAuthManager> authStore = authStoreFile == null
            ? new InMemoryAuthStore<>()
            : new GsonFileAuthStore<>(
                new AuthenticatorCodec(httpClient),
                new ReentrantLock(),
                VERSION,
                authStoreFile
            );

        return new McAuthProvider(httpClient, providerName, authStore);
    }

    protected McAuthProvider(HttpClient httpClient, String providerName, AuthStore<JavaAuthManager> authStore) {
        this.httpClient = httpClient;
        this.authStore = authStore;
        this.name = providerName;
        this.authManagerBuilder = JavaAuthManager.create(httpClient);
    }

    @Override
    public synchronized Account refresh(Account account) {
        return refresh(account, true);
    }

    /**
     * Only contacts the authentication services if the Minecraft token is about to expire,
     * see {@link #MIN_TOKEN_VALIDITY_MS}, so that launching the game repeatedly does not
     * log in on every launch.
     */
    @Override
    public synchronized Account refreshIfNeeded(Account account) {
        return refresh(account, false);
    }

    private Account refresh(Account account, boolean force) {
        // use generics to make this type safe?
        if (!getName().equals(account.getProvider())) {
            throw new IllegalArgumentException(
                "Cannot refresh account for provider " + account.getProvider() + " with provider " + getName()
            );
        }

        Map<String, JavaAuthManager> accounts = authStore.read();
        JavaAuthManager authManager = accounts.get(account.getName());
        if (authManager == null) {
            throw new NotFoundException("Failed to find account " + account.getName());
        }

        try {
            if (!force && !needsRefresh(authManager)) {
                return account(authManager);
            }

            refreshMinecraftToken(authManager);
            authManager.getMinecraftProfile().refresh();
            Account result = account(authManager);
            authStore.add(account.getName(), authManager);
            return result;
        } catch (IOException e) {
            throw new AuthException(e);
        }
    }

    @Override
    public Optional<Account> getAccount(String name) {
        Optional<JavaAuthManager> account = authStore.getById(name);
        return account.map(this::account);
    }

    @Override
    public List<Account> getAccounts() {
        return authStore.stream().map(this::account).collect(Collectors.toList());
    }

    @Override
    public List<Account> getLoadedAccounts() {
        List<Account> result = new ArrayList<>();
        for (JavaAuthManager authManager : authStore.loaded()) {
            MinecraftProfile profile = authManager.getMinecraftProfile().getCached();
            MinecraftToken token = authManager.getMinecraftToken().getCached();
            if (profile != null && token != null) {
                result.add(account(profile, token));
            }
        }

        return result;
    }

    @Override
    public void removeAccount(Account account) {
        authStore.remove(account.getName());
    }

    @Override
    public Authenticator getAuthenticator() {
        return new McAuthenticator(this);
    }

    synchronized Account addAccount(JavaAuthManager authManager) {
        Account account = account(authManager);
        authStore.add(account.getName(), authManager);
        return account;
    }

    private static boolean needsRefresh(JavaAuthManager authManager) {
        MinecraftToken token = authManager.getMinecraftToken().getCached();
        return token == null
            || token.getExpireTimeMs() - System.currentTimeMillis() < MIN_TOKEN_VALIDITY_MS
            || !authManager.getMinecraftProfile().hasValue();
    }

    /**
     * Requests a new Minecraft token.
     *
     * <p>The request is made with the cached XSTS token as long as that has not expired locally.
     * The Minecraft services can reject such a token with a 401 before that,
     * in which case the XSTS token is refreshed and the request is made once more.</p>
     */
    private static void refreshMinecraftToken(JavaAuthManager authManager) throws IOException {
        boolean usesCachedXstsToken = !authManager.getJavaXstsToken().isExpired();
        try {
            authManager.getMinecraftToken().refresh();
        } catch (HttpRequestException e) {
            // an XSTS token that has just been requested for this attempt is not going to be replaced by a better one
            if (!usesCachedXstsToken || e.getResponse().getStatusCode() != StatusCodes.UNAUTHORIZED) {
                throw e;
            }

            try {
                authManager.getJavaXstsToken().refresh();
                authManager.getMinecraftToken().refresh();
            } catch (IOException retryException) {
                retryException.addSuppressed(e);
                throw retryException;
            }
        }
    }

    private Account account(JavaAuthManager authManager) {
        try {
            MinecraftProfile profile = authManager.getMinecraftProfile().getCached();
            if (profile == null) {
                profile = authManager.getMinecraftProfile().getUpToDate();
            }

            MinecraftToken token = authManager.getMinecraftToken().getCached();
            if (token == null) {
                token = authManager.getMinecraftToken().getUpToDate();
            }

            return account(profile, token);
        } catch (IOException e) {
            throw new AuthException("Failed to get Account " + authManager, e);
        }
    }

    private Account account(MinecraftProfile profile, MinecraftToken token) {
        return new Account(
            getName(),
            profile.getName(),
            profile.getId().toString(),
            token.getToken(),
            Account.TYPE_MSA,
            "" // xuid not supported for now
        );
    }

}

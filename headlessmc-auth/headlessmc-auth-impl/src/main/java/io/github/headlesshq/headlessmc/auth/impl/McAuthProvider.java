package io.github.headlesshq.headlessmc.auth.impl;

import io.github.headlesshq.headlessmc.auth.*;
import io.github.headlesshq.headlessmc.auth.store.gson.GsonFileAuthStore;
import io.github.headlesshq.headlessmc.exceptions.NotFoundException;
import lombok.Getter;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.java.model.MinecraftProfile;
import net.raphimc.minecraftauth.java.model.MinecraftToken;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Getter
public class McAuthProvider implements AuthProvider {
    private static final long VERSION = 0L;

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
            authManager.getMinecraftToken().refresh();
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

            return new Account(
                getName(),
                profile.getName(),
                profile.getId().toString(),
                token.getToken(),
                Account.TYPE_MSA,
                "" // xuid not supported for now
            );
        } catch (IOException e) {
            throw new AuthException("Failed to get Account " + authManager, e);
        }
    }

}

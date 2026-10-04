package io.github.headlesshq.headlessmc.auth.cdi;

import com.google.gson.reflect.TypeToken;
import io.github.headlesshq.headlessmc.auth.*;
import io.github.headlesshq.headlessmc.auth.impl.McAuthProvider;
import io.github.headlesshq.headlessmc.auth.offline.OfflineAuthProvider;
import io.github.headlesshq.headlessmc.auth.store.gson.GsonCodec;
import io.github.headlesshq.headlessmc.auth.store.gson.GsonCodecImpl;
import io.github.headlesshq.headlessmc.auth.store.gson.GsonFileAuthStore;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.net.NetConfig;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@ApplicationScoped
@RegisterForReflection(targets = {Account.class, AccountInfo.class}) // for GSON to serialize
public class AuthFactory {
    @Produces
    @Default
    @ApplicationScoped
    public AuthProvider createDefaultAuthProvider(Holder<NetConfig> config, FileService fileService, AppFiles appFiles) {
        return McAuthProvider.of(
            AuthProvider.DEFAULT,
            config.get().userAgent(),
            fileService.getPath(appFiles.getAuthDir(), AuthProvider.DEFAULT, ".accounts.json")
        );
    }

    @Produces
    @Offline
    @ApplicationScoped
    public AuthStore<Account> createOfflineAccountStore(FileService fileService, AppFiles appFiles) {
        GsonCodec<Account> codec = new GsonCodecImpl<>(Account.class);
        Path file = fileService.getPath(appFiles.getAuthDir(), AuthProvider.OFFLINE, ".accounts.json");
        return new GsonFileAuthStore<>(codec, new ReentrantLock(), OfflineAuthProvider.VERSION, file);
    }

    @Produces
    @Offline
    @ApplicationScoped
    public AuthProvider createOfflineAuthProvider(@Offline AuthStore<Account> authStore) {
        Logger logger = LoggerFactory.getLogger(OfflineAuthProvider.class);
        return new OfflineAuthProvider(
            authStore,
            throwable -> logger.error("Offline auth", throwable),
            AuthProvider.OFFLINE,
            OfflineAuthProvider.VERSION
        );
    }

    @Produces
    @Default
    @ApplicationScoped
    public AuthService createAuthService(
        @Any Instance<AuthProvider> providers,
        @Default AuthProvider defaultProvider
    ) {
        return new AuthServiceImpl(providers.stream().toList(), defaultProvider);
    }

    @Produces
    @Default
    @ApplicationScoped
    public LastUsedAccountService createLastUsedAccountService(
        AuthService authService,
        FileService fileService,
        AppFiles appFiles
    ) {
        Logger logger = LoggerFactory.getLogger(LastUsedAccountServiceImpl.class);
        Path file = fileService.getPath(appFiles.getAuthDir(), "last", ".accounts.json");
        //noinspection Convert2Diamond
        GsonCodec<List<AccountInfo>> codec = new GsonCodecImpl<>(new TypeToken<List<AccountInfo>>() {});
        AuthStore<List<AccountInfo>> store = new GsonFileAuthStore<>(
            codec,
            new ReentrantLock(),
            LastUsedAccountServiceImpl.VERSION,
            file
        );

        return new LastUsedAccountServiceImpl(
            store,
            authService,
            logger::error
        );
    }

}

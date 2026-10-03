package io.github.headlesshq.headlessmc.auth.yggdrasil;

import io.github.headlesshq.headlessmc.auth.AuthException;
import io.github.headlesshq.headlessmc.auth.Authenticator;
import io.github.headlesshq.headlessmc.auth.Credentials;
import io.github.headlesshq.headlessmc.console.Console;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// TODO: https://github.com/xiaofanforfabric/headlessmc/blob/main/headlessmc-auth/src/main/java/io/github/headlesshq/headlessmc/auth/YggdrasilClient.java
@RequiredArgsConstructor
final class YggdrasilAuthenticator implements Authenticator {
    private final YggdrasilAuthProvider provider;

    // TODO:

    @Override
    public @Unmodifiable Map<String, Method<Console>> getMethods() {
        Map<String, Method<Console>> result = new HashMap<>();
        return result;
    }

    @Override
    public Optional<Method<Credentials>> loginWithCredentials() throws AuthException {
        return Authenticator.super.loginWithCredentials();
    }

}

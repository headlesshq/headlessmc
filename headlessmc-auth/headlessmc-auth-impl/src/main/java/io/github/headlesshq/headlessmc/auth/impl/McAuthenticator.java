package io.github.headlesshq.headlessmc.auth.impl;

import io.github.headlesshq.headlessmc.auth.AuthException;
import io.github.headlesshq.headlessmc.auth.Authenticator;
import io.github.headlesshq.headlessmc.auth.Credentials;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;
import lombok.RequiredArgsConstructor;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.msa.model.MsaCredentials;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.service.impl.CredentialsMsaAuthService;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;
import net.raphimc.minecraftauth.msa.service.impl.JfxWebViewMsaAuthService;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@RequiredArgsConstructor
final class McAuthenticator implements Authenticator {
    private final McAuthProvider provider;

    @Override
    public @Unmodifiable Map<String, Method<Console>> getMethods() {
        Map<String, Method<Console>> result = new LinkedHashMap<>();
        result.put(Authenticator.DEFAULT_METHOD, loginWithCode());
        // TODO: currently unsupported by native image result.put(Authenticator.METHOD_WEBVIEW, loginWithWebview());
        result.put(Authenticator.METHOD_CREDENTIALS, loginWithInteractiveCredentials()
            .orElseThrow(() -> new IllegalStateException("Failed to find method " + METHOD_CREDENTIALS)));
        return result;
    }

    @Override
    public Optional<Method<Credentials>> loginWithCredentials() throws AuthException {
        return Optional.of(credentials -> {
            try {
                JavaAuthManager authManager = provider.authManagerBuilder.login(
                    CredentialsMsaAuthService::new,
                    new MsaCredentials(credentials.getEmail(), credentials.getPassword())
                );

                return provider.addAccount(authManager);
            } catch (IOException | TimeoutException e) {
                throw new AuthException(e);
            } catch (InterruptedException e) {
                throw new UncheckedInterruptedException(e);
            }
        });
    }

    private Method<Console> loginWithCode() throws AuthException {
        return output -> {
            try {
                JavaAuthManager authManager = provider.authManagerBuilder.login(
                    DeviceCodeMsaAuthService::new, (Consumer<MsaDeviceCode>) deviceCode ->
                        output.write("To login visit " + deviceCode.getDirectVerificationUri())
                );

                return provider.addAccount(authManager);
            } catch (IOException | TimeoutException e) {
                throw new AuthException(e);
            } catch (InterruptedException e) {
                throw new UncheckedInterruptedException(e);
            }
        };
    }

    private Method<Console> loginWithWebview() throws AuthException {
        return console -> {
            try {
                JavaAuthManager authManager = provider.authManagerBuilder.login(JfxWebViewMsaAuthService::new);
                return provider.addAccount(authManager);
            } catch (IOException | TimeoutException e) {
                throw new AuthException(e);
            } catch (InterruptedException e) {
                throw new UncheckedInterruptedException(e);
            } catch (UncheckedInterruptedException e) {
                throw e;
            } catch (/* okay-to-catch-marker */OutOfMemoryError error) {
                throw error;
            } catch (/* okay-to-catch-marker */Throwable throwable) { // JavaFX may not be supported
                throw new AuthException(throwable);
            }
        };
    }

}

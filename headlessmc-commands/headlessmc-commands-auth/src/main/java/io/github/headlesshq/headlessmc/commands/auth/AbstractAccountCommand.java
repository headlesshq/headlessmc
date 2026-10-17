package io.github.headlesshq.headlessmc.commands.auth;

import io.github.headlesshq.headlessmc.auth.AnyAuthProvider;
import io.github.headlesshq.headlessmc.auth.AuthProvider;
import io.github.headlesshq.headlessmc.auth.AuthService;
import io.github.headlesshq.headlessmc.auth.LastUsedAccountService;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.util.Collections;
import java.util.List;

/**
 * Base class for {@link AccountCommand} and {@link LoginShortcutCommand},
 * provides the {@code --provider} option and the {@link AccountCommandContext}.
 */
@Getter
@Setter
@RequiredArgsConstructor
public abstract class AbstractAccountCommand implements AccountCommandContext, Runnable {
    private final LastUsedAccountService lastUsedAccountService;
    private final TableProvider tableProvider;
    private final AuthService authService;
    private final Console console;

    @CommandLine.Option(
        names = {"-p", "--provider"},
        description = "The authentication provider to use (default, offline, yggdrasil, ...)",
        completionCandidates = AuthProviderCompletions.class
    )
    private @Nullable String provider;

    @Override
    public AuthProvider getAuthProvider() {
        String provider = getProvider();
        if (provider == null || "any".equalsIgnoreCase(provider)) {
            return new AnyAuthProvider(authService);
        }

        return authService.getProvider(provider)
            .orElseThrow(() -> new IllegalArgumentException("Failed to find authentication provider " + provider));
    }

    @Override
    public List<AuthProvider> getAuthProviders() {
        String provider = getProvider();
        if (provider == null || "any".equalsIgnoreCase(provider)) {
            return authService.getProviders();
        }

        return Collections.singletonList(authService.getProvider(provider)
            .orElseThrow(() -> new IllegalArgumentException("Failed to find authentication provider " + provider)));
    }

}

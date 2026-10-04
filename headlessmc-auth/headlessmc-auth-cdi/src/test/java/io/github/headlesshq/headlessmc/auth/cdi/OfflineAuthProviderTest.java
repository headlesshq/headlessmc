package io.github.headlesshq.headlessmc.auth.cdi;

import io.github.headlesshq.headlessmc.auth.AuthProvider;
import io.github.headlesshq.headlessmc.auth.AuthService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class OfflineAuthProviderTest {
    @Inject
    @Offline
    AuthProvider authProvider;

    @Inject
    AuthService authService;

    @Test
    public void testSameness() {
        Optional<AuthProvider> provider = authService.getProvider(AuthProvider.OFFLINE);
        assertTrue(provider.isPresent());
        assertEquals(authProvider, provider.get());
    }


}

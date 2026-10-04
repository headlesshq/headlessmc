package io.github.headlesshq.headlessmc.platform.vanilla;

import io.github.headlesshq.headlessmc.platform.client.ClientInstaller;
import io.github.headlesshq.headlessmc.platform.Vanilla;
import io.github.headlesshq.headlessmc.platform.VanillaInstaller;
import io.github.headlesshq.headlessmc.platform.server.ServerInstaller;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static io.smallrye.common.constraint.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@QuarkusTest
public class VanillaInstallerTest {
    @Inject
    @Vanilla
    ClientInstaller clientInstaller;

    @Inject
    @Vanilla
    ServerInstaller serverInstaller;

    @Inject
    @Vanilla
    VanillaInstaller vanillaInstaller;

    // Unsatisfied dependency: no bean matches the injection point
    // raise with IntelliJ? This is satisifed...
    @Inject
    VanillaInstaller defaultVanillaInstaller;

    @Test
    public void testVanillaInstallerInjection() {
        assertNotNull(clientInstaller);
        assertSame(clientInstaller, serverInstaller);
        assertSame(clientInstaller, vanillaInstaller);
        assertSame(clientInstaller, defaultVanillaInstaller);
    }

}

package io.github.headlesshq.headlessmc.platform.fabric;

import io.github.headlesshq.headlessmc.platform.client.ClientInstaller;
import io.github.headlesshq.headlessmc.platform.server.ServerInstaller;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@QuarkusTest
public class FabricInstallerTest {
    @Inject
    @Fabric
    ServerInstaller serverInstaller;

    @Inject
    @Fabric
    ClientInstaller clientInstaller;

    @Test
    public void testFabricInstaller() {
        assertNotNull(serverInstaller);
        assertSame(clientInstaller, serverInstaller);
    }

}

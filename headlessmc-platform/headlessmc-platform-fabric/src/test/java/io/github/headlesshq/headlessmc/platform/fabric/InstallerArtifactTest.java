package io.github.headlesshq.headlessmc.platform.fabric;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class InstallerArtifactTest {
    @Inject
    InstallerArtifactService installerArtifactService;

    @Test
    public void testInstallerArtifacts() {
        InstallerArtifact installer = installerArtifactService.getInstaller();
        assertEquals("net.fabricmc", installer.group());
        assertEquals("fabric-installer", installer.name());
        assertTrue(installer.getURL().toString().startsWith(installer.repository()));
    }

    @Test
    public void testLegacyInstaller() {
        InstallerArtifact legacy = installerArtifactService.getLegacyInstaller();
        assertEquals("net.legacyfabric", legacy.group());
        assertEquals("fabric-installer", legacy.name());
        assertTrue(legacy.getURL().toString().startsWith(legacy.repository()));
    }

}

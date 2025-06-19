package io.github.headlesshq.headlessmc.platform.vanilla;

import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.github.headlesshq.headlessmc.cditestfixtures.WeldTestInitializer.getWeld;
import static org.junit.jupiter.api.Assertions.*;

@EnableWeld
@ExtendWith(WeldJunit5Extension.class)
public class VanillaVersionManagerTest {
    @WeldSetup
    private final WeldInitiator weldInitiator = WeldInitiator.from(getWeld(TestDownloadService.class)).build();

    @Inject
    private VanillaVersionManager vanillaVersionManager;

    @Test
    @SneakyThrows
    public void testVanillaVersionService() {
        VanillaVersionManager manager = vanillaVersionManager;
        VanillaVersion version = manager.getVersion("1.18"); // we have excluded some versions from the version.json
        assertNull(version);
        version = manager.getVersion("1.21.5");
        assertNotNull(version);
        assertEquals("1.21.5", version.getName());
        assertEquals("release", version.getType());
        assertTrue(version.getUrl().startsWith("https://piston-meta.mojang.com/"));
    }

    @Test
    public void testOlderThan() {
        VanillaVersionManager manager = vanillaVersionManager;
        assertThrows(IllegalArgumentException.class, () -> manager.isOlderThan("doesNotExist", "1.21.5"));
        assertThrows(IllegalArgumentException.class, () -> manager.isOlderThan("1.21.5", "doesNotExist"));
        assertTrue(manager.isOlderThan("1.21.5", "25w14craftmine"));
        assertTrue(manager.isOlderThan("1.21.4", "1.21.5"));
        assertTrue(manager.isOlderThan("1.21", "1.21.5"));
        assertTrue(manager.isOlderThan("rd-132211", "a1.0.11"));
        assertTrue(manager.isOlderThan("b1.4", "1.12.2"));
        assertFalse(manager.isOlderThan("1.21.5", "1.21.4"));
        assertFalse(manager.isOlderThan("1.21", "1.12"));
        assertFalse(manager.isOlderThan("1.12.2", "b1.6.3"));
    }

}

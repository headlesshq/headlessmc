package io.github.headlesshq.headlessmc.launcher.command.forge;

import io.github.headlesshq.headlessmc.launcher.LauncherMock;
import io.github.headlesshq.headlessmc.launcher.UsesResources;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ForgeIndexCacheTest extends ForgeIndexCache
    implements UsesResources {

    public ForgeIndexCacheTest() {
        super(LauncherMock.INSTANCE, LEX_FORGE_INDICES);
    }

    @Test
    public void testRead() throws IOException {
        val versions = this.read(getJsonElement("forge_index.json"));

        assertEquals("41.0.110", versions.get(0).getName());
        assertEquals("41.0.109", versions.get(1).getName());
        assertEquals("41.0.108", versions.get(2).getName());
        assertEquals("41.0.107", versions.get(3).getName());
        assertEquals("11.14.3.1543", versions.get(4).getName());

        assertEquals("1.19", versions.get(0).getVersion());
        assertEquals("1.19", versions.get(1).getVersion());
        assertEquals("1.19", versions.get(2).getVersion());
        assertEquals("1.19", versions.get(3).getVersion());
        assertEquals("1.8", versions.get(4).getVersion());

        assertEquals("1.19-41.0.110", versions.get(0).getFullName());
        assertEquals("1.19-41.0.109", versions.get(1).getFullName());
        assertEquals("1.19-41.0.108", versions.get(2).getFullName());
        assertEquals("1.19-41.0.107", versions.get(3).getFullName());
        assertEquals("1.8-11.14.3.1543", versions.get(4).getFullName());
    }

}

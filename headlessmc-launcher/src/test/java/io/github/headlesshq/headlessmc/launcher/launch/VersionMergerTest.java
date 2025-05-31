package io.github.headlesshq.headlessmc.launcher.launch;

import io.github.headlesshq.headlessmc.launcher.UsesResources;
import io.github.headlesshq.headlessmc.launcher.version.Version;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VersionMergerTest implements UsesResources {
    @Test
    public void testVersionMerger() {
        val v = setupVersions("version_child.json", "version_parent.json");
        val m = new VersionMerger(v[0]);

        assertEquals(v[1].getJava(), m.getJava());
        assertEquals(v[1].getAssets(), m.getAssets());
        assertEquals(v[1].getAssetsUrl(), m.getAssetsUrl());
        assertEquals(v[1].getClientDownload(), m.getClientDownload());

        assertEquals(v[0].getName(), m.getName());
        assertEquals(v[0].getFolder(), m.getFolder());
        assertEquals(v[0].getParentName(), m.getParentName());
        assertEquals(v[0].getType(), m.getType());
        assertEquals(v[0].isNewArgumentFormat(), m.isNewArgumentFormat());
        assertTrue(m.isNewArgumentFormat());

        assertEquals(v[0].getMainClass(), m.getMainClass());
        assertNotEquals(v[1].getMainClass(), m.getMainClass());

        v[0].getLibraries()
            .forEach(l -> assertTrue(m.getLibraries().contains(l)));
        v[1].getLibraries()
            .forEach(l -> assertTrue(m.getLibraries().contains(l)));

        v[0].getArguments()
            .forEach(arg -> assertTrue(m.getArguments().contains(arg)));
        v[1].getArguments()
            .forEach(arg -> assertTrue(m.getArguments().contains(arg)));
    }

    @Test
    public void testVersionMergerOldArgFormat() {
        val v = setupVersions("version_child_old.json",
                              "version_parent_old.json");
        val m = new VersionMerger(v[0]);
        assertFalse(v[0].isNewArgumentFormat());
        assertFalse(v[1].isNewArgumentFormat());
        assertFalse(m.isNewArgumentFormat());

        assertEquals(m.getArguments(), v[0].getArguments());
        assertNotEquals(m.getArguments(), v[1].getArguments());
    }

    @SneakyThrows
    private Version[] setupVersions(String child, String parent) {
        val version1 = getVersion(child, "child_file", 0);
        val version2 = getVersion(parent, "parent_file", 1);
        version1.setParent(version2);
        return new Version[]{version1, version2};
    }

}

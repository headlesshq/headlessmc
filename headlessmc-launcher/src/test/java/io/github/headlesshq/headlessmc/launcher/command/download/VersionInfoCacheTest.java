package io.github.headlesshq.headlessmc.launcher.command.download;

import io.github.headlesshq.headlessmc.launcher.UsesResources;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VersionInfoCacheTest implements UsesResources {
    @Test
    @SneakyThrows
    public void testRead() {
        val vic = new VersionInfoCache();
        val json = getJsonObject("versions.json");
        vic.read(json);

        Assertions.assertEquals("1.19", vic.getLatestRelease());
        Assertions.assertEquals("1.19.1-rc1", vic.getLatestSnapshot());
        Assertions.assertEquals(4, vic.size());

        VersionInfo info = vic.get(0);
        Assertions.assertEquals(0, info.getId());
        Assertions.assertEquals(vic.getLatestSnapshot(), info.getName());
        Assertions.assertEquals("snapshot", info.getType());
        Assertions.assertEquals("http://1.19.1-rc1-url", info.getUrl());

        info = vic.get(1);
        Assertions.assertEquals(1, info.getId());
        Assertions.assertEquals(vic.getLatestRelease(), info.getName());
        Assertions.assertEquals("release", info.getType());
        Assertions.assertEquals("http://1.19-url", info.getUrl());

        info = vic.get(2);
        Assertions.assertEquals(2, info.getId());
        Assertions.assertEquals("rd-132211", info.getName());
        Assertions.assertEquals("old_alpha", info.getType());
        Assertions.assertEquals("http://rd-132211-url", info.getUrl());

        info = vic.get(3);
        Assertions.assertEquals(3, info.getId());
        Assertions.assertEquals("b1.6.1", info.getName());
        Assertions.assertEquals("old_beta", info.getType());
        Assertions.assertEquals("http://b1.6.1-url", info.getUrl());
    }

}

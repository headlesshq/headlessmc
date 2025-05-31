package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.JsonObject;
import io.github.headlesshq.headlessmc.version.Download;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DownloadsParserTest implements UsesResources {
    @Test
    public void testParse() {
        DownloadsParser parser = new DownloadsParser();
        JsonObject element = getJsonObject("downloads.json");
        Map<String, Download> downloads = parser.parseDownloads(element);
        assertNotNull(downloads);
        assertEquals(2, downloads.size());

        Download download = downloads.get("client");
        assertNotNull(download);
        assertEquals("client", download.getName());
        assertEquals("b88808bbb3da8d9f453694b5d8f74a3396f1a533", download.getSha1());
        assertEquals(28984409, download.getSize());
        assertEquals(URI.create("https://piston-meta.mojang.com"), download.getUrl());

        download = downloads.get("client_mappings");
        assertNotNull(download);
        assertEquals("client_mappings", download.getName());
        assertNull(download.getSha1());
        assertNull(download.getSize());
        assertNull(download.getUrl());
    }

}

package io.github.headlesshq.headlessmc.version.parser;

import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.version.Features;
import io.github.headlesshq.headlessmc.version.Library;
import io.github.headlesshq.headlessmc.version.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LibraryParserTest implements UsesResources, ParsesLibraries {
    @Test
    public void test() {
        List<Library> libs = parse(getJsonObject("lib.json"));
        assertEquals(1, libs.size());
        OS os = new OS("Windows", OS.Type.WINDOWS, "10", "x64", false);
        Features feat = Features.EMPTY;
        assertEquals(Rule.Action.ALLOW, libs.get(0).getRule().apply(os, feat));
        assertEquals("testpackage:test:testversion", libs.get(0).getName());
        assertEquals("testpackage", libs.get(0).getPackage());
        assertEquals("test", libs.get(0).getNameAfterPackage());
        assertEquals("testversion", libs.get(0).getVersionNumber());
        assertEquals("http://_download_url", libs.get(0).getUrl("").toString());
        Assertions.assertFalse(libs.get(0).isNativeLibrary());
    }

    @Test
    public void testLibWithNatives() {
        List<Library> libs = parse(getJsonObject("lib_natives.json"));
        assertEquals(2, libs.size());
        Assertions.assertFalse(libs.get(0).isNativeLibrary());
        Assertions.assertTrue(libs.get(1).isNativeLibrary());
    }

}

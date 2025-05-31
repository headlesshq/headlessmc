package io.github.headlesshq.headlessmc.version.parser;

import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.version.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LibraryImplTest {
    @Test
    public void testGetPath() {
        Map<String, String> natives = new HashMap<>();
        OS _64 = new OS("windows", OS.Type.WINDOWS, "11", "x64", true);
        OS _32 = new OS("windows", OS.Type.WINDOWS, "11", "x86", false);

        LibraryImpl lib = new LibraryImpl(
            natives, null, "test:test:test",
            Rule.ALLOW, "baseUrl", "url", null, null, "path", true);
        Assertions.assertEquals("path", lib.getPath(_64));

        lib = new LibraryImpl(
            natives, null, "test:test:test",
            Rule.ALLOW, "baseUrl", "url", null, null, "path-${arch}", true);
        Assertions.assertEquals("path-64", lib.getPath(_64));
        Assertions.assertEquals("path-32", lib.getPath(_32));

        lib = new LibraryImpl(
            natives, null, "test:test:test-${arch}",
            Rule.ALLOW, "baseUrl", "url", null, null, null, false);
        Assertions.assertEquals(
            String.join(File.separator, "test", "test",
                        "test-${arch}", "test-test-${arch}.jar"),
            lib.getPath(_64));

        lib = new LibraryImpl(
            natives, null, "test:test:test-${arch}",
            Rule.ALLOW, "baseUrl", "url", null, null, null, true);
        Assertions.assertEquals(
            String.join(File.separator, "test", "test",
                        "test-${arch}", "test-test-${arch}.jar"),
            lib.getPath(_64));

        natives.put(_32.getType().getName(), "natives-${arch}");
        Assertions.assertEquals(
            String.join(File.separator, "test", "test",
                        "test-${arch}", "test-test-${arch}-natives-32.jar"),
            lib.getPath(_32));
        Assertions.assertEquals(
            String.join(File.separator, "test", "test",
                        "test-${arch}", "test-test-${arch}-natives-64.jar"),
            lib.getPath(_64));
    }

}

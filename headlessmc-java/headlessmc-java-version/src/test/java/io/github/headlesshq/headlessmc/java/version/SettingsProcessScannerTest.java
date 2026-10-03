package io.github.headlesshq.headlessmc.java.version;

import io.github.headlesshq.headlessmc.java.Java;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class SettingsProcessScannerTest {
    @Inject
    @SettingsProcessScanner.Settings
    SettingsProcessScanner settingsProcessScanner;

    @Test
    public void testSystemPropertyParser() throws IOException {
        String output = """
            Property settings:
                file.encoding = UTF-8
                file.separator = /
                java.class.path =
                java.class.version = 69.0
                java.specification.version = 25
        """;

        assertEquals(25, settingsProcessScanner.parseOutput(output));
    }

    @Test
    public void testParseOldVersion() throws IOException {
        String output = """
            Property settings:
                java.specification.version = 1.8
        """;

        assertEquals(8, settingsProcessScanner.parseOutput(output));
    }

    @Test
    public void testParseAndroidVersion() throws IOException {
        String output = """
            Property settings:
                java.specification.version = 0.9
        """;

        assertEquals(Java.JAVA_VERSION_0_9, settingsProcessScanner.parseOutput(output));
    }

}

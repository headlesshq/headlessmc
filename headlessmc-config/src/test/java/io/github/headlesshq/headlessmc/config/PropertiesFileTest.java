package io.github.headlesshq.headlessmc.config;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class PropertiesFileTest {
    @TempDir
    Path dir;

    private Path file() {
        return dir.resolve("config.properties");
    }

    private void write(String content) throws Exception {
        Files.writeString(file(), content, StandardCharsets.UTF_8);
    }

    private String read() throws Exception {
        return Files.readString(file(), StandardCharsets.UTF_8);
    }

    @Test
    public void loadOfAMissingFileIsEmpty() throws Exception {
        assertEquals(Map.of(), PropertiesFile.load(file()));
    }

    @Test
    public void loadReadsUtf8() throws Exception {
        write("# comment\nkey=välue ✓\nother : x\n");
        assertEquals(Map.of("key", "välue ✓", "other", "x"), PropertiesFile.load(file()));
    }

    @Test
    public void setCreatesTheFileAndItsParents() throws Exception {
        Path file = dir.resolve("a").resolve("b").resolve("config.properties");
        PropertiesFile.set(file, "key", "value");
        assertEquals(Map.of("key", "value"), PropertiesFile.load(file));
    }

    @Test
    public void setAppendsAndKeepsCommentsBlankLinesAndOrder() throws Exception {
        write("# === HeadlessMc Config ===\n# hmc.jline.enabled=true\n\nfirst=1\nsecond=2\n");
        PropertiesFile.set(file(), "third", "3");
        assertEquals("# === HeadlessMc Config ===\n# hmc.jline.enabled=true\n\nfirst=1\nsecond=2\nthird=3\n", read());
    }

    @Test
    public void setDoesNotTreatCommentedOutPropertiesAsEntries() throws Exception {
        write("# hmc.jline.enabled=true\n! hmc.jline.enabled=true\n");
        PropertiesFile.set(file(), "hmc.jline.enabled", "false");
        assertEquals("# hmc.jline.enabled=true\n! hmc.jline.enabled=true\n\nhmc.jline.enabled=false\n", read());
    }

    @Test
    public void setAppendsAfterAFileWithoutTrailingNewline() throws Exception {
        write("first=1");
        PropertiesFile.set(file(), "second", "2");
        assertEquals("first=1\nsecond=2\n", read());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"key=old|key=new", "key = old|key = new", "key:old|key:new", "key old|key new"})
    public void setReplacesExistingEntriesInPlaceKeepingTheirSeparator(String line, String expected) throws Exception {
        write("# header\nbefore=1\n" + line + "\nafter=2\n");
        PropertiesFile.set(file(), "key", "new");
        assertEquals("# header\nbefore=1\n" + expected + "\nafter=2\n", read());
    }

    @ParameterizedTest
    @ValueSource(strings = {"  key : old", "\tkey=old"})
    public void indentedEntriesAreOverriddenByAnAppendedEntry(String line) throws Exception {
        // Commons Configuration does not recognize indented keys, it keeps the line and appends a new entry,
        // which wins when reading, as the last entry for a key wins
        write(line + "\n");
        PropertiesFile.set(file(), "key", "new");
        assertEquals(line + "\nkey=new\n", read());
        assertEquals(Map.of("key", "new"), PropertiesFile.load(file()));
    }

    @Test
    public void setOnAFileWithOnlyCommentsKeepsThemAtTheTop() throws Exception {
        write("# === HeadlessMc Config ===\n# hmc.jline.enabled=true\n");
        PropertiesFile.set(file(), "hmc.java.download", "false");
        assertEquals("# === HeadlessMc Config ===\n# hmc.jline.enabled=true\n\nhmc.java.download=false\n", read());
    }

    @Test
    public void setKeepsCommentsAboveTheirEntries() throws Exception {
        write("a=1\n\n# about b\nb=2\n");
        PropertiesFile.set(file(), "b", "3");
        PropertiesFile.set(file(), "c", "4");
        assertEquals("a=1\n\n# about b\nb=3\nc=4\n", read());
    }

    @Test
    public void setDoesNotReplaceKeysThatOnlyShareAPrefix() throws Exception {
        write("key.suffix=1\nkeyx=2\n");
        PropertiesFile.set(file(), "key", "3");
        assertEquals("key.suffix=1\nkeyx=2\nkey=3\n", read());
    }

    @Test
    public void setReplacesAllPhysicalLinesOfAMultiLineEntry() throws Exception {
        write("before=1\nkey=first \\\n    second \\\n    third\nafter=2\n");
        assertEquals("first second third", PropertiesFile.load(file()).get("key"));
        PropertiesFile.set(file(), "key", "new");
        assertEquals("before=1\nkey=new\nafter=2\n", read());
    }

    @Test
    public void anEscapedBackslashAtTheEndOfALineIsNotAContinuation() throws Exception {
        write("path=C:\\\\\nkey=old\n");
        PropertiesFile.set(file(), "key", "new");
        assertEquals("path=C:\\\\\nkey=new\n", read());
        assertEquals(Map.of("path", "C:\\", "key", "new"), PropertiesFile.load(file()));
    }

    @Test
    public void aCommentEndingWithABackslashIsNotAContinuation() throws Exception {
        write("# comment \\\nkey=old\n");
        PropertiesFile.set(file(), "key", "new");
        assertEquals("# comment \\\nkey=new\n", read());
    }

    @Test
    public void setReplacesDuplicatesWithASingleEntry() throws Exception {
        write("key=1\nother=x\nkey=2\n");
        PropertiesFile.set(file(), "key", "3");
        assertEquals("key=3\nother=x\n", read());
    }

    @Test
    public void setWorksWithWindowsLineSeparators() throws Exception {
        write("# header\r\nkey=old\r\nother=x\r\n");
        PropertiesFile.set(file(), "key", "new");
        assertEquals(List.of("# header", "key=new", "other=x"), read().lines().toList());
        assertEquals(Map.of("key", "new", "other", "x"), PropertiesFile.load(file()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "  leading spaces",
        "trailing spaces  ",
        "back\\slash",
        "C:\\Users\\me\\.minecraft",
        "multi\nline",
        "with=equals:and colon",
        "#not a comment",
        "unicode ✓ äöü",
        "ends with backslash\\"
    })
    public void valuesRoundTrip(String value) throws Exception {
        write("# header\n");
        PropertiesFile.set(file(), "key", value);
        assertEquals(Map.of("key", value), PropertiesFile.load(file()));
        // and again, replacing the existing entry
        PropertiesFile.set(file(), "key", value + "!");
        assertEquals(Map.of("key", value + "!"), PropertiesFile.load(file()));
        assertTrue(read().startsWith("# header"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"key with spaces", "key=with:separators", "ключ"})
    public void keysRoundTrip(String key) throws Exception {
        PropertiesFile.set(file(), key, "1");
        PropertiesFile.set(file(), key, "2");
        assertEquals(Map.of(key, "2"), PropertiesFile.load(file()));
    }

    @Test
    public void setFailsOnMalformedFilesAndLeavesThemAsTheyAre() throws Exception {
        write("broken=\\uZZZZ\nkey=old\n");
        assertThrows(ConfigurationException.class, () -> PropertiesFile.set(file(), "key", "new"));
        assertEquals("broken=\\uZZZZ\nkey=old\n", read());
    }

    @Test
    public void setLeavesNoTemporaryFilesBehind() throws Exception {
        PropertiesFile.set(file(), "key", "value");
        PropertiesFile.set(file(), "key", "other");
        try (Stream<Path> files = Files.list(dir)) {
            assertEquals(List.of(file()), files.toList());
        }
    }

    @Test
    public void setFailsIfTheFileIsADirectory() throws Exception {
        Files.createDirectories(file());
        assertThrows(ConfigurationException.class, () -> PropertiesFile.set(file(), "key", "value"));
    }

}

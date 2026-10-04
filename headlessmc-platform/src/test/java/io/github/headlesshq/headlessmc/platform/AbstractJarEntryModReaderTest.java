package io.github.headlesshq.headlessmc.platform;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.platform.mods.Mod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class AbstractJarEntryModReaderTest {
    /** Reads a mod id from the plain-text content of the matched jar entries. */
    private static class TestReader extends AbstractJarEntryModReader {
        private final boolean stopAfterFound;

        TestReader(boolean stopAfterFound, String... names) {
            super(names);
            this.stopAfterFound = stopAfterFound;
        }

        @Override
        public Optional<List<Mod>> readEntry(InputStream inputStream) {
            try {
                String id = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
                if (id.isEmpty()) {
                    return Optional.empty();
                }

                return Optional.of(List.of(new Mod(id, id, Optional.empty(), List.of(), Map.of())));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        protected boolean stopAfterFoundEntry() {
            return stopAfterFound;
        }
    }

    @TempDir
    Path root;

    private Path jar(Map<String, String> entries) {
        Path file = root.resolve("mod-" + entries.hashCode() + ".jar");
        try (OutputStream out = Files.newOutputStream(file);
             JarOutputStream jar = new JarOutputStream(out)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                jar.putNextEntry(new JarEntry(entry.getKey()));
                jar.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                jar.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return file;
    }

    @Test
    void entryNamesAreExposed() {
        assertEquals(List.of("a.json", "b.json"), List.copyOf(new TestReader(false, "a.json", "b.json").getEntryNames()));
    }

    @Test
    void readsAllMatchingEntries() {
        Path jar = jar(Map.of("a.json", "mod-a", "b.json", "mod-b"));

        List<Mod> mods = new TestReader(false, "a.json", "b.json").read(jar);

        assertEquals(List.of("mod-a", "mod-b"), mods.stream().map(Mod::id).sorted().toList());
    }

    @Test
    void stopsAfterTheFirstEntryWhenRequested() {
        Path jar = jar(Map.of("a.json", "mod-a", "b.json", "mod-b"));

        List<Mod> mods = new TestReader(true, "a.json", "b.json").read(jar);

        assertEquals(List.of("mod-a"), mods.stream().map(Mod::id).toList());
    }

    @Test
    void missingEntriesAreSkipped() {
        Path jar = jar(Map.of("b.json", "mod-b"));

        assertEquals(List.of("mod-b"), new TestReader(false, "a.json", "b.json").read(jar).stream().map(Mod::id).toList());
    }

    @Test
    void jarWithoutMatchingEntriesYieldsNoMods() {
        assertEquals(List.of(), new TestReader(false, "a.json").read(jar(Map.of("other.txt", "x"))));
    }

    @Test
    void emptyEntryContentIsIgnored() {
        assertEquals(List.of(), new TestReader(true, "a.json").read(jar(Map.of("a.json", ""))));
    }

    @Test
    void unreadableJarThrows() {
        assertThrows(HeadlessMcIOException.class, () -> new TestReader(false, "a.json").read(root.resolve("nope.jar")));
    }

}

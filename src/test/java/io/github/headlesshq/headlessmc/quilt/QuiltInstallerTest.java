package io.github.headlesshq.headlessmc.quilt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class QuiltInstallerTest {
    @Test
    void testDeleteEmptyVersionJars(@TempDir Path versions) throws IOException {
        Path emptyQuilt = jar(versions, "quilt-loader-0.30.1-1.21.1", new byte[0]);
        Path quilt = jar(versions, "quilt-loader-0.30.1-1.20.1", new byte[]{1});
        Path emptyOther = jar(versions, "fabric-loader-0.19.5-1.21.1", new byte[0]);

        QuiltInstaller.deleteEmptyVersionJars(versions);

        assertFalse(Files.exists(emptyQuilt));
        assertTrue(Files.exists(emptyQuilt.resolveSibling("quilt-loader-0.30.1-1.21.1.json")));
        assertTrue(Files.exists(quilt));
        assertTrue(Files.exists(emptyOther));
        QuiltInstaller.deleteEmptyVersionJars(versions.resolve("missing"));
    }

    private static Path jar(Path versions, String id, byte[] content) throws IOException {
        Path dir = Files.createDirectories(versions.resolve(id));
        Files.writeString(dir.resolve(id + ".json"), "{}");
        return Files.write(dir.resolve(id + ".jar"), content);
    }

}

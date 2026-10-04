package io.github.headlesshq.headlessmc.launcher.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class EulaTest {
    private static final String MOJANG_EULA =
        "#By changing the setting below to TRUE you are indicating your agreement to our EULA.\n"
            + "#Wed Aug 06 00:00:00 CEST 2026\n"
            + "eula=false\n";

    @Test
    public void acceptsDefaultEulaFile(@TempDir Path dir) throws IOException {
        Path file = Files.writeString(dir.resolve("eula.txt"), MOJANG_EULA);
        Eula eula = new Eula(file);
        assertFalse(eula.isAccepted());

        eula.accept();

        assertTrue(eula.isAccepted());
        assertTrue(Files.readString(file).contains("eula=true"));
        assertFalse(Files.readString(file).contains("eula=false"));
    }

    @Test
    public void acceptingAcceptedEulaIsANoOp(@TempDir Path dir) throws IOException {
        Path file = Files.writeString(dir.resolve("eula.txt"), "eula=true\n");
        Eula eula = new Eula(file);

        eula.accept();

        assertTrue(eula.isAccepted());
    }

    @Test
    public void acceptThrowsWithoutEulaLine(@TempDir Path dir) throws IOException {
        Path file = Files.writeString(dir.resolve("eula.txt"), "something=else\n");
        assertThrows(ServerException.class, () -> new Eula(file).accept());
    }

    @Test
    public void missingFileIsNotAccepted(@TempDir Path dir) {
        Eula eula = new Eula(dir.resolve("eula.txt"));
        assertFalse(eula.exists());
        assertFalse(eula.isAccepted());
        assertThrows(ServerException.class, eula::read);
    }

    @Test
    public void readReturnsContent(@TempDir Path dir) throws IOException {
        Path file = Files.writeString(dir.resolve("eula.txt"), "line1\nline2");
        assertEquals("line1\nline2", new Eula(file).read());
    }

}

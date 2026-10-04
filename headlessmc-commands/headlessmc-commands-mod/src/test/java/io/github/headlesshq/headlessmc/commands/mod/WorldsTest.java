package io.github.headlesshq.headlessmc.commands.mod;

import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorldsTest {
    @TempDir
    Path root;

    private Profile client() {
        return new Profile("main", VersionArg.parse("fabric", "1.21.1"), root.resolve("main"));
    }

    private Profile server() {
        return new Profile("srv", VersionArg.parse("server", "1.21.1"), root.resolve("srv"));
    }

    private Path dir(Path path) throws IOException {
        return Files.createDirectories(path);
    }

    @Test
    void listWorldsWithoutSavesDirIsEmpty() {
        assertEquals(List.of(), Worlds.listWorlds(client()));
    }

    @Test
    void listWorldsOfClientReadsSavesDir() throws IOException {
        Profile profile = client();
        Path b = dir(profile.path().resolve(Worlds.SAVES).resolve("b"));
        Path a = dir(profile.path().resolve(Worlds.SAVES).resolve("a"));
        Files.createFile(profile.path().resolve(Worlds.SAVES).resolve("not-a-world.txt"));

        assertEquals(List.of(a, b), Worlds.listWorlds(profile));
    }

    @Test
    void listWorldsOfServerRequiresDatapacksDir() throws IOException {
        Profile profile = server();
        Path world = dir(profile.path().resolve("world"));
        dir(world.resolve(Worlds.DATAPACKS));
        dir(profile.path().resolve("logs"));

        assertEquals(List.of(world), Worlds.listWorlds(profile));
    }

    @Test
    void resolveNamedClientWorld() throws IOException {
        Profile profile = client();
        Path world = dir(profile.path().resolve(Worlds.SAVES).resolve("New World"));

        assertEquals(world, Worlds.resolveWorld(profile, "New World"));
    }

    @Test
    void resolveUnknownClientWorldThrows() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> Worlds.resolveWorld(client(), "nope")
        );
        assertTrue(e.getMessage().contains("Failed to find world 'nope'"));
    }

    @Test
    void resolveSingleClientWorldWithoutName() throws IOException {
        Profile profile = client();
        Path world = dir(profile.path().resolve(Worlds.SAVES).resolve("only"));

        assertEquals(world, Worlds.resolveWorld(profile, null));
    }

    @Test
    void resolveAmbiguousClientWorldThrows() throws IOException {
        Profile profile = client();
        dir(profile.path().resolve(Worlds.SAVES).resolve("a"));
        dir(profile.path().resolve(Worlds.SAVES).resolve("b"));

        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> Worlds.resolveWorld(profile, null)
        );
        assertTrue(e.getMessage().contains("found 2 worlds"));
    }

    @Test
    void resolveClientWorldWithoutSavesThrows() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> Worlds.resolveWorld(client(), null)
        );
        assertTrue(e.getMessage().contains("found 0 worlds"));
    }

    @Test
    void resolveNamedServerWorld() throws IOException {
        Profile profile = server();
        Path world = dir(profile.path().resolve("custom"));

        assertEquals(world, Worlds.resolveWorld(profile, "custom"));
    }

    @Test
    void resolveUnknownServerWorldThrows() {
        assertThrows(IllegalArgumentException.class, () -> Worlds.resolveWorld(server(), "nope"));
    }

    @Test
    void resolveDefaultServerWorld() throws IOException {
        Profile profile = server();
        Path world = dir(profile.path().resolve(Worlds.DEFAULT_SERVER_WORLD));

        assertEquals(world, Worlds.resolveWorld(profile, null));
    }

    @Test
    void resolveMissingDefaultServerWorldThrows() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> Worlds.resolveWorld(server(), null)
        );
        assertTrue(e.getMessage().contains("no default 'world' world found"));
    }

}

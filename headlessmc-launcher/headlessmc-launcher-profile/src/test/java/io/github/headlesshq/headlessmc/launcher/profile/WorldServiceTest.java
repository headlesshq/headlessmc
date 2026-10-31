package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorldServiceTest {
    private final WorldService worlds = new WorldService();

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
        assertEquals(List.of(), worlds.listWorlds(client()));
    }

    @Test
    void listWorldsOfClientReadsSavesDir() throws IOException {
        Profile profile = client();
        Path b = dir(profile.path().resolve(WorldService.SAVES).resolve("b"));
        Path a = dir(profile.path().resolve(WorldService.SAVES).resolve("a"));
        Files.createFile(profile.path().resolve(WorldService.SAVES).resolve("not-a-world.txt"));

        assertEquals(List.of(a, b), worlds.listWorlds(profile));
    }

    @Test
    void listWorldsOfServerRequiresDatapacksDir() throws IOException {
        Profile profile = server();
        Path world = dir(profile.path().resolve("world"));
        dir(world.resolve(WorldService.DATAPACKS));
        dir(profile.path().resolve("logs"));

        assertEquals(List.of(world), worlds.listWorlds(profile));
    }

    @Test
    void resolveNamedClientWorld() throws IOException {
        Profile profile = client();
        Path world = dir(profile.path().resolve(WorldService.SAVES).resolve("New World"));

        assertEquals(world, worlds.resolveWorld(profile, "New World"));
    }

    @Test
    void resolveUnknownClientWorldThrows() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> worlds.resolveWorld(client(), "nope")
        );
        assertTrue(e.getMessage().contains("Failed to find world 'nope'"));
    }

    @Test
    void resolveSingleClientWorldWithoutName() throws IOException {
        Profile profile = client();
        Path world = dir(profile.path().resolve(WorldService.SAVES).resolve("only"));

        assertEquals(world, worlds.resolveWorld(profile, null));
    }

    @Test
    void resolveAmbiguousClientWorldThrows() throws IOException {
        Profile profile = client();
        dir(profile.path().resolve(WorldService.SAVES).resolve("a"));
        dir(profile.path().resolve(WorldService.SAVES).resolve("b"));

        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> worlds.resolveWorld(profile, null)
        );
        assertTrue(e.getMessage().contains("found 2 worlds"));
    }

    @Test
    void resolveClientWorldWithoutSavesThrows() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> worlds.resolveWorld(client(), null)
        );
        assertTrue(e.getMessage().contains("found 0 worlds"));
    }

    @Test
    void resolveNamedServerWorld() throws IOException {
        Profile profile = server();
        Path world = dir(profile.path().resolve("custom"));

        assertEquals(world, worlds.resolveWorld(profile, "custom"));
    }

    @Test
    void resolveUnknownServerWorldThrows() {
        assertThrows(IllegalArgumentException.class, () -> worlds.resolveWorld(server(), "nope"));
    }

    @Test
    void resolveDefaultServerWorld() throws IOException {
        Profile profile = server();
        Path world = dir(profile.path().resolve(WorldService.DEFAULT_SERVER_WORLD));

        assertEquals(world, worlds.resolveWorld(profile, null));
    }

    @Test
    void listWorldsOfServerFindsLevelDat() throws IOException {
        Profile profile = server();
        Path world = dir(profile.path().resolve("world"));
        dir(world.resolve(WorldService.DATAPACKS));
        Path nether = dir(profile.path().resolve("world_nether"));
        Files.createFile(nether.resolve(WorldService.LEVEL_DAT));

        assertEquals(List.of(world, nether), worlds.listWorlds(profile));
    }

    @Test
    void resolveDefaultServerWorldFromServerProperties() throws IOException {
        Profile profile = server();
        Path world = dir(profile.path().resolve("survival"));
        Files.writeString(profile.path().resolve(WorldService.SERVER_PROPERTIES), "level-name=survival\n");

        assertEquals("survival", worlds.getDefaultServerWorldName(profile));
        assertEquals(world, worlds.resolveWorld(profile, null));
    }

    @Test
    void blankLevelNameFallsBackToDefaultWorld() throws IOException {
        Profile profile = server();
        dir(profile.path());
        Files.writeString(profile.path().resolve(WorldService.SERVER_PROPERTIES), "level-name=\n");

        assertEquals(WorldService.DEFAULT_SERVER_WORLD, worlds.getDefaultServerWorldName(profile));
    }

    @Test
    void resolveMissingDefaultServerWorldThrows() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> worlds.resolveWorld(server(), null)
        );
        assertTrue(e.getMessage().contains("no default 'world' world found"));
    }

}

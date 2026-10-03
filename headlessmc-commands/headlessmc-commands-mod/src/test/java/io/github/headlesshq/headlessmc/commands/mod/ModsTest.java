package io.github.headlesshq.headlessmc.commands.mod;

import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.mods.ModSupport;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ModsTest {
    @TempDir
    Path root;

    private final Set<ModType> allTypes = Set.of(
        ModType.MOD, ModType.PLUGIN, ModType.RESOURCE_PACK, ModType.MOD_PACK, ModType.DATA_PACK, ModType.SHADER
    );

    private FakePlatform fabric;
    private FakePlatformService platformService;

    @BeforeEach
    void setup() {
        fabric = FakePlatform.create("fabric", new String[]{"1.21.1", "0.16.9"});
        fabric.withModSupport(new ModSupport(List.of(ModType.MOD, ModType.RESOURCE_PACK), List.of()));
        platformService = new FakePlatformService(new FakeVanillaPlatform("1.21.1"), fabric);
    }

    private Profile profile(String platform) {
        return new Profile("main", VersionArg.parse(platform, "1.21.1"), root.resolve("main"));
    }

    @Test
    void getPlatformResolvesByProfileVersion() {
        assertSame(fabric, Mods.getPlatform(platformService, profile("fabric")));
    }

    @Test
    void getUnknownPlatformThrows() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> Mods.getPlatform(platformService, profile("forge"))
        );
        assertTrue(e.getMessage().contains("Failed to find platform forge"));
    }

    @Test
    void getModSupportOfSupportingPlatform() {
        assertEquals(List.of(ModType.MOD, ModType.RESOURCE_PACK), Mods.getModSupport(platformService, profile("fabric")).modTypes());
    }

    @Test
    void getModSupportOfUnsupportingPlatformThrows() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> Mods.getModSupport(platformService, profile("vanilla"))
        );
        assertTrue(e.getMessage().contains("does not support mods"));
    }

    @Test
    void resolveTypeByName() {
        assertEquals(Set.of(ModType.DATA_PACK), Mods.resolveType(platformService, allTypes, profile("fabric"), "DataPack"));
    }

    @Test
    void resolveUnknownTypeThrows() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> Mods.resolveType(platformService, allTypes, profile("fabric"), "nope"));
        assertTrue(e.getMessage().contains("Unknown mod type 'nope'"));
    }

    @Test
    void resolveDefaultTypeUsesFirstOfPlatformAndModPacks() {
        assertEquals(Set.of(ModType.MOD, ModType.MOD_PACK), Mods.resolveType(platformService, allTypes, profile("fabric"), null));
    }

    @Test
    void resolveDefaultTypeWithoutModTypesThrows() {
        fabric.withModSupport(new ModSupport(List.of(), List.of()));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> Mods.resolveType(platformService, allTypes, profile("fabric"), null));
        assertTrue(e.getMessage().contains("does not declare a default mod type"));
    }

    @Test
    void targetDirOfNonDatapacksIsTheGameDir() {
        Profile profile = profile("fabric");
        assertEquals(profile.path(), Mods.targetDir(profile, Set.of(ModType.MOD), null));
        assertEquals(profile.path(), Mods.targetDir(profile, Set.of(ModType.MOD_PACK), null));
    }

    @Test
    void targetDirOfDatapackIsInWorld() throws IOException {
        Profile profile = profile("fabric");
        Path world = Files.createDirectories(profile.path().resolve(Worlds.SAVES).resolve("New World"));

        assertEquals(world.resolve(Worlds.DATAPACKS), Mods.targetDir(profile, Set.of(ModType.DATA_PACK), "New World"));
    }

    @Test
    void isDatapack() {
        assertTrue(Mods.isDatapack(ModType.DATA_PACK));
        assertFalse(Mods.isDatapack(ModType.MOD));
    }

}

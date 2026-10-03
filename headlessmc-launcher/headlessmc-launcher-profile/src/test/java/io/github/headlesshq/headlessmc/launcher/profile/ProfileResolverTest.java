package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProfileResolverTest {
    @TempDir
    Path root;

    private FakePlatformService platformService;
    private FakeProfileService profileService;
    private ProfileResolver resolver;

    @BeforeEach
    void setup() {
        FakePlatform fabric = FakePlatform.create("fabric", new String[]{"1.21.1", "0.16.9"});
        platformService = new FakePlatformService(new FakeVanillaPlatform("1.21.1", "1.20.4"), fabric);
        profileService = new FakeProfileService(root);
        resolver = new ProfileResolver(platformService, profileService);
    }

    @Test
    void resolvesAProfileByName() {
        Profile profile = profileService.createDefault("main", VersionArg.parse("fabric", "1.21.1"));

        assertEquals(profile, resolver.resolve(List.of("main"), Side.BOTH));
    }

    @Test
    void profilesOfTheWrongSideAreRejected() {
        profileService.createDefault("main", VersionArg.parse("fabric", "1.21.1"));

        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> resolver.resolve(List.of("main"), Set.of(Side.SERVER))
        );
        assertTrue(e.getMessage().contains("but it was for client"));
    }

    @Test
    void unknownServerProfilesAreRejected() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> resolver.resolve(List.of("nope"), Set.of(Side.SERVER))
        );
        assertTrue(e.getMessage().contains("Failed to find profile/server <nope>"));
    }

    @Test
    void versionArgumentsAreResolvedToProfiles() {
        Profile profile = resolver.resolve(List.of("fabric", "1.21.1"), Side.BOTH);

        assertEquals("fabric", profile.version().platform());
        assertEquals("1.21.1", profile.version().version());
        assertEquals(Optional.empty(), profile.version().side());
    }

    @Test
    void theSideIsAppliedWhenOnlyOneIsAllowed() {
        Profile profile = resolver.resolve(List.of("fabric", "1.21.1"), Set.of(Side.CLIENT));

        assertEquals(Optional.of(Side.CLIENT), profile.version().side());
    }

    @Test
    void unknownVersionsAreRejected() {
        assertThrows(RuntimeException.class, () -> resolver.resolve(List.of("fabric", "1.7.10"), Side.BOTH));
    }

    @Test
    void serverProfilesAreFoundForTheServerSide() {
        Profile server = profileService.createDefault("srv", VersionArg.parse("server", "fabric", "1.21.1"));

        assertEquals(server, resolver.resolve(List.of("srv"), Set.of(Side.SERVER)));
        assertEquals(server, resolver.resolve(List.of("srv"), Side.BOTH));
    }

}

package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
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
    private LaunchTargetService launchTargetService;
    private ProfileResolver resolver;

    @BeforeEach
    void setup() {
        FakePlatform fabric = FakePlatform.create("fabric", new String[]{"1.21.1", "0.16.9"});
        platformService = new FakePlatformService(new FakeVanillaPlatform("1.21.1", "1.20.4"), fabric);
        profileService = new FakeProfileService(root);
        launchTargetService = TestLaunchTargets.versions(platformService, profileService, root);
        resolver = new ProfileResolver(profileService, launchTargetService);
    }

    private VersionArg version(Profile profile) {
        return launchTargetService.describe(profile).specified().orElseThrow();
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

        assertEquals(VersionHandle.VERSION, profile.target().name());
        assertEquals("fabric", version(profile).platform());
        assertEquals("1.21.1", version(profile).version());
        assertEquals(Optional.empty(), version(profile).side());
        // a client, because nothing says otherwise
        assertEquals(Optional.of(Side.CLIENT), profile.side());
        assertEquals("fabric-1.21.1", profile.name());
    }

    @Test
    void aProfileCreatedForArgumentsIsNotStored() {
        Profile profile = resolver.resolve(List.of("fabric", "1.21.1"), Side.BOTH);

        assertEquals(Optional.empty(), profileService.getProfile(profile.name()));
        assertEquals(List.of(), profileService.getProfiles());
    }

    @Test
    void theSideOfTheProfileIsTheSideOfItsTarget() {
        Profile client = resolver.resolve(List.of("fabric", "1.21.1"), Set.of(Side.CLIENT));
        Profile server = resolver.resolve(List.of("server", "fabric", "1.21.1"), Side.BOTH);

        assertEquals(Optional.of(Side.CLIENT), client.side());
        assertEquals(Optional.of(Side.SERVER), server.side());
        assertEquals(VersionHandle.SERVER, server.target().name());
        assertEquals(Optional.of(Side.SERVER), version(server).side());
    }

    @Test
    void unknownVersionsAreRejected() {
        assertThrows(RuntimeException.class, () -> resolver.resolve(List.of("fabric", "1.7.10"), Side.BOTH));
    }

    @Test
    void theErrorForAnUnknownSingleArgumentMentionsProfiles() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> resolver.resolve(List.of("nope"), Side.BOTH)
        );
        assertTrue(e.getMessage().contains("Failed to find profile 'nope'"), e.getMessage());
        // and why no launch target took it
        assertTrue(e.getMessage().contains("version:"), e.getMessage());
    }

    @Test
    void serverProfilesAreFoundForTheServerSide() {
        Profile server = profileService.createDefault("srv", VersionArg.parse("server", "fabric", "1.21.1"));

        assertEquals(server, resolver.resolve(List.of("srv"), Set.of(Side.SERVER)));
        assertEquals(server, resolver.resolve(List.of("srv"), Side.BOTH));
    }

    @Test
    void aStoredProfileNameWinsOverAVersionOfThatName() {
        // profiles for versions are named after their version, e.g. "1.21.1"
        Profile named = profileService.createDefault("1.21.1", VersionArg.parse("fabric", "1.21.1"));

        assertEquals(named, resolver.resolve(List.of("1.21.1"), Side.BOTH));
    }

    @Test
    void aStoredProfileForTheSameVersionIsReused() {
        Profile stored = profileService.createDefault("my-fabric", VersionArg.parse("fabric", "1.21.1"));
        profileService.createDefault("my-vanilla", VersionArg.parse("1.21.1"));

        assertEquals(stored, resolver.resolve(List.of("fabric", "1.21.1"), Side.BOTH));
        // not for another build of that version
        assertNotEquals(stored, resolver.resolve(List.of("fabric", "1.21.1", "0.16.9"), Side.BOTH));
    }

    @Test
    void aStoredServerIsNotUsedForTheClientOfTheSameVersion() {
        profileService.createDefault("srv", VersionArg.parse("server", "fabric", "1.21.1"));

        Profile client = resolver.resolve(List.of("fabric", "1.21.1"), Side.BOTH);

        assertEquals(Optional.of(Side.CLIENT), client.side());
        assertNotEquals("srv", client.name());
    }

    @Test
    void serverCommandsUseStoredServersForVersionsButDoNotCreateThem() {
        Profile server = profileService.createDefault("srv", VersionArg.parse("server", "fabric", "1.21.1"));

        // for a command that only launches servers the "server" is not needed
        assertEquals(server, resolver.resolve(List.of("fabric", "1.21.1"), Set.of(Side.SERVER)));
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> resolver.resolve(List.of("1.20.4"), Set.of(Side.SERVER))
        );
        assertTrue(e.getMessage().contains("Failed to find profile/server <1.20.4>"), e.getMessage());
    }

    @Test
    void aNamedTargetIsUsedInsteadOfAProfileWithThatName() {
        profileService.createDefault("1.21.1", VersionArg.parse("fabric", "1.21.1"));

        Profile profile = resolver.resolve(VersionHandle.VERSION, List.of("1.21.1"), Side.BOTH, new TypedMapImpl());

        // the vanilla version, not the fabric profile that happens to be named like it
        assertEquals(VersionArg.PLATFORM_VANILLA, version(profile).platform());
    }

    @Test
    void theGameDirectoryOptionIsUsedForNewProfiles() {
        TypedMapImpl options = new TypedMapImpl();
        options.put(LaunchTarget.GAME_DIR, root.resolve("custom"));

        Profile profile = resolver.resolve(null, List.of("fabric", "1.21.1"), Side.BOTH, options);

        assertEquals(root.resolve("custom"), profile.path());
    }

    @Test
    void emptyArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(List.of(), Side.BOTH));
    }

    @Test
    void theVersionOfAProfileCanBeAsked() {
        Profile profile = profileService.createDefault("main", VersionArg.parse("fabric", "1.21.1"));

        assertEquals(VersionArg.parse("fabric", "1.21.1"), resolver.requireVersion(profile));
    }

}

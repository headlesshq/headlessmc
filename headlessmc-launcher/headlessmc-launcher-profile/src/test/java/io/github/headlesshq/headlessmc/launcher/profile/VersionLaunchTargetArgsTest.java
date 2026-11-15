package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
import io.github.headlesshq.headlessmc.platform.server.ServerSupport;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What the launch targets for versions accept as arguments, see {@link AbstractVersionLaunchTarget}.
 * Arguments are only accepted if they resolve to a version that exists.
 */
class VersionLaunchTargetArgsTest {
    @TempDir
    Path root;

    private FakePlatformService platformService;
    private FakeVersionLaunchTarget client;
    private FakeVersionLaunchTarget server;

    @BeforeEach
    void setup() {
        // fabric supports both sides, paper only servers
        FakePlatform fabric = FakePlatform.create("fabric", new String[]{"1.21.1", "0.16.10", "0.16.9"})
            .withClientSupport(new ClientSupport(null, (id, mcFiles, args) -> new FakeVersion("fabric")))
            .withServerSupport(new ServerSupport(null, null));
        FakePlatform paper = FakePlatform.create("paper", new String[]{"1.21.1", "130"})
            .withServerSupport(new ServerSupport(null, null));
        platformService = new FakePlatformService(new FakeVanillaPlatform("1.21.1", "1.20.4"), fabric, paper);
        DefaultJacksonJsonService jsonService = new DefaultJacksonJsonService();
        client = new FakeVersionLaunchTarget(platformService, jsonService, Side.CLIENT, root);
        server = new FakeVersionLaunchTarget(platformService, jsonService, Side.SERVER, root);
    }

    private static TargetArgs serverOnly(String... args) {
        return new TargetArgs(List.of(args), Set.of(Side.SERVER));
    }

    private static TargetArgs clientOnly(String... args) {
        return new TargetArgs(List.of(args), Set.of(Side.CLIENT));
    }

    // names

    @Test
    void theTargetsAreNamedAfterWhatTheyLaunch() {
        assertEquals("version", client.getName());
        assertEquals("server", server.getName());
        assertEquals("[client] [platform] <mc-version> [build]", client.getUsage());
        assertEquals("[server] [platform] <mc-version> [build]", server.getUsage());
        assertTrue(client.isDetectable());
    }

    // parsing: the client target

    @Test
    void clientParsesVersionsWithoutSideAndWithClient() {
        assertEquals(VersionArg.parse("1.21.1"), client.handle(client.parse(TargetArgs.of("1.21.1")).handle()).version());
        assertEquals(
            VersionArg.parse("fabric", "1.21.1"),
            client.handle(client.parse(TargetArgs.of("fabric", "1.21.1")).handle()).version()
        );
        assertEquals(
            VersionArg.parse("fabric", "1.21.1", "0.16.9"),
            client.handle(client.parse(TargetArgs.of("client", "fabric", "1.21.1", "0.16.9")).handle()).version()
        );
        assertEquals(Optional.of(Side.CLIENT), client.parse(TargetArgs.of("1.21.1")).side());
    }

    @Test
    void theSideIsNotPartOfTheVersionOfAClient() {
        VersionHandle handle = client.handle(client.parse(TargetArgs.of("client", "1.21.1")).handle());

        // the side is a property of the profile
        assertEquals(Optional.empty(), handle.version().side());
        assertFalse(handle.isServer());
    }

    @Test
    void aParsedHandleIsNotResolved() {
        VersionHandle handle = client.handle(client.parse(TargetArgs.of("fabric", "1.21.1")).handle());

        assertNull(handle.current());
        assertNull(handle.currentTarget());
    }

    @Test
    void whatHasBeenSpecifiedIsKept() {
        // "latest" exists, but it is not replaced with the build that is the latest right now
        VersionHandle handle = client.handle(client.parse(TargetArgs.of("fabric", "1.21.1", "latest")).handle());

        assertEquals(Optional.of("latest"), handle.version().build());
        assertEquals("fabric", handle.version().platform());
    }

    @Test
    void platformNamesAreNormalized() {
        VersionHandle handle = client.handle(client.parse(TargetArgs.of("FABRIC", "1.21.1")).handle());

        assertEquals("fabric", handle.version().platform());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "quilt 1.21.1",          // no such platform
        "fabric 1.7.10",         // no such Minecraft version
        "1.7.10",
        "fabric 1.21.1 0.0.1",   // no such build
        "my-custom-pack",        // the name of an installed version.json is not a version
        "versions/1.21.1/1.21.1.json",
        "./paper.jar",
        "a b c d e"
    })
    void argumentsThatDoNotResolveToAVersionAreRejected(String args) {
        TargetArgs targetArgs = TargetArgs.of(args.split(" "));

        assertThrows(IllegalArgumentException.class, () -> client.parse(targetArgs));
        assertThrows(IllegalArgumentException.class, () -> server.parse(serverOnly(args.split(" "))));
    }

    @Test
    void theReasonForRejectingArgumentsIsGiven() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> client.parse(TargetArgs.of("quilt", "1.21.1")))
                       .getMessage().contains("Failed to find platform quilt"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> client.parse(TargetArgs.of()))
                       .getMessage().contains("no version specified"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> client.parse(TargetArgs.of("a", "b", "c", "d", "e")))
                       .getMessage().contains("is not a version"));
    }

    @Test
    void clientRejectsServerVersions() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> client.parse(TargetArgs.of("server", "1.21.1"))
        );

        assertTrue(e.getMessage().contains("is a server version"), e.getMessage());
    }

    @Test
    void clientIsNotUsedByCommandsThatOnlyLaunchServers() {
        assertThrows(IllegalArgumentException.class, () -> client.parse(serverOnly("1.21.1")));
        assertEquals(List.of(), client.complete(serverOnly("")));
    }

    // parsing: the server target

    @Test
    void serverParsesVersionsStartingWithServer() {
        ParsedTarget parsed = server.parse(TargetArgs.of("server", "fabric", "1.21.1"));

        assertEquals(Optional.of(Side.SERVER), parsed.side());
        VersionHandle handle = server.handle(parsed.handle());
        assertEquals(VersionArg.parse("server", "fabric", "1.21.1"), handle.version());
        assertTrue(handle.isServer());
    }

    @Test
    void aVersionWithoutSideIsAClientUnlessTheCommandOnlyLaunchesServers() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> server.parse(TargetArgs.of("fabric", "1.21.1"))
        );
        assertTrue(e.getMessage().contains("use 'server fabric 1.21.1'"), e.getMessage());

        // e.g. "server add fabric 1.21.1"
        VersionHandle handle = server.handle(server.parse(serverOnly("fabric", "1.21.1")).handle());
        assertEquals(VersionArg.parse("server", "fabric", "1.21.1"), handle.version());
    }

    @Test
    void serverRejectsClientVersions() {
        assertThrows(IllegalArgumentException.class, () -> server.parse(TargetArgs.of("client", "1.21.1")));
        assertThrows(IllegalArgumentException.class, () -> server.parse(clientOnly("server", "1.21.1")));
    }

    @Test
    void exactlyOneOfTheTwoTargetsTakesAVersion() {
        for (String[] args : List.of(
            new String[]{"1.21.1"}, new String[]{"fabric", "1.21.1"}, new String[]{"client", "fabric", "1.21.1"},
            new String[]{"server", "1.21.1"}, new String[]{"server", "fabric", "1.21.1", "0.16.9"}
        )) {
            boolean isClient = parses(client, TargetArgs.of(args));
            boolean isServer = parses(server, TargetArgs.of(args));
            assertTrue(isClient ^ isServer, String.join(" ", args));
        }
    }

    private static boolean parses(LaunchTarget target, TargetArgs args) {
        try {
            target.parse(args);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // platforms have to support the side

    @Test
    void theRealTargetsRequireThePlatformToSupportTheirSide() {
        FakeVersionLaunchTarget strict = new FakeVersionLaunchTarget(
            platformService, new DefaultJacksonJsonService(), Side.CLIENT, root
        ) {
            @Override
            protected boolean isSupported(io.github.headlesshq.headlessmc.platform.VersionID id) {
                return id.getPlatform().hasSupportFor(getSide());
            }
        };

        // paper has no clients
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> strict.parse(TargetArgs.of("paper", "1.21.1"))
        );
        assertTrue(e.getMessage().contains("platform paper does not support clients"), e.getMessage());
        assertDoesNotThrow(() -> strict.parse(TargetArgs.of("fabric", "1.21.1")));
    }

    // describing

    @Test
    void describesTheVersionOfAProfile() {
        Profile profile = new Profile("p", VersionArg.parse("fabric", "1.21.1", "latest"), root);
        VersionHandle resolved = client.handle(profile)
            .withCurrent(VersionArg.parse("fabric", "1.21.1", "0.16.10"))
            .withCurrentTarget(root.resolve("fabric.json"));

        TargetInfo unresolved = client.describe(profile);
        TargetInfo info = client.describe(profile.withTarget(client.handle(resolved)));

        assertEquals(Optional.of(VersionArg.parse("fabric", "1.21.1", "latest")), unresolved.specified());
        assertEquals(Optional.empty(), unresolved.current());
        assertEquals(Optional.empty(), unresolved.file());
        assertEquals(Optional.of(VersionArg.parse("fabric", "1.21.1", "0.16.10")), info.current());
        assertEquals(Optional.of(root.resolve("fabric.json")), info.file());
        assertEquals(info.current(), info.version());
        assertEquals("version", client.getLabel(profile));
    }

    @Test
    void aVagueMinecraftVersionIsNotAKnownVersion() {
        TargetInfo latest = new TargetInfo(Optional.of(VersionArg.parse("fabric", "latest")), Optional.empty(), Optional.empty());
        TargetInfo stable = new TargetInfo(Optional.of(VersionArg.parse("stable")), Optional.empty(), Optional.empty());
        TargetInfo resolved = new TargetInfo(
            Optional.of(VersionArg.parse("fabric", "latest")), Optional.of(VersionArg.parse("fabric", "1.21.1")), Optional.empty()
        );

        assertFalse(latest.isVersionKnown());
        assertFalse(stable.isVersionKnown());
        assertTrue(resolved.isVersionKnown());
        assertFalse(TargetInfo.EMPTY.isVersionKnown());
        assertEquals(Optional.empty(), TargetInfo.EMPTY.version());
    }

    @Test
    void theSameVersionIsTheSameTarget() {
        Profile stored = new Profile("p", VersionArg.parse("fabric", "1.21.1"), root);
        // what a profile has been resolved to does not matter
        Profile resolved = stored.withTarget(client.handle(
            client.handle(stored).withCurrent(VersionArg.parse("fabric", "1.21.1", "0.16.9"))
        ));

        assertTrue(client.isSameTarget(resolved, client.parse(TargetArgs.of("fabric", "1.21.1"))));
        assertTrue(client.isSameTarget(resolved, client.parse(TargetArgs.of("client", "FABRIC", "1.21.1"))));
        assertFalse(client.isSameTarget(resolved, client.parse(TargetArgs.of("fabric", "1.21.1", "0.16.9"))));
        assertFalse(client.isSameTarget(resolved, client.parse(TargetArgs.of("1.21.1"))));
    }

    // new profiles

    @Test
    void profilesAreNamedAfterTheirVersion() {
        assertEquals(
            Optional.of("fabric-1.21.1"),
            client.defaultName(client.parse(TargetArgs.of("fabric", "1.21.1")).handle())
        );
        assertEquals(Optional.of("1.21.1"), client.defaultName(client.parse(TargetArgs.of("1.21.1")).handle()));
        assertEquals(
            Optional.of("fabric-1.21.1-latest"),
            client.defaultName(client.parse(TargetArgs.of("fabric", "1.21.1", "latest")).handle())
        );
        assertEquals(
            Optional.of("server-fabric-1.21.1"),
            server.defaultName(server.parse(serverOnly("fabric", "1.21.1")).handle())
        );
    }

    @Test
    void createsAProfileWithTheDefaultsOfTheSide() {
        Profile clientProfile = client.create("c", client.parse(TargetArgs.of("fabric", "1.21.1")), new TypedMapImpl());
        Profile serverProfile = server.create("s", server.parse(serverOnly("fabric", "1.21.1")), new TypedMapImpl());

        assertEquals(Optional.of(Side.CLIENT), clientProfile.side());
        assertEquals(root.resolve("c"), clientProfile.path());
        assertEquals(List.of(), clientProfile.gameArgs());
        assertEquals("version", clientProfile.target().name());
        assertEquals(Optional.of(Side.SERVER), serverProfile.side());
        assertEquals(List.of("nogui"), serverProfile.gameArgs());
        assertEquals("server", serverProfile.target().name());
        assertEquals(Profile.PROFILE_VERSION, clientProfile.hmcVersion());
    }

    @Test
    void aGivenGameDirectoryIsUsed() {
        TypedMap options = new TypedMapImpl();
        options.put(LaunchTarget.GAME_DIR, root.resolve("custom"));

        assertEquals(
            root.resolve("custom"),
            client.create("c", client.parse(TargetArgs.of("1.21.1")), options).path()
        );
    }

    @Test
    void theConvenienceConstructorOfProfileMatchesTheTargets() {
        Profile clientProfile = new Profile("c", VersionArg.parse("client", "fabric", "1.21.1"), root);
        Profile serverProfile = new Profile("s", VersionArg.parse("server", "fabric", "1.21.1"), root);

        assertEquals(Optional.of(Side.CLIENT), clientProfile.side());
        assertEquals("version", clientProfile.target().name());
        assertEquals(VersionArg.parse("fabric", "1.21.1"), client.handle(clientProfile).version());
        assertEquals(Optional.of(Side.SERVER), serverProfile.side());
        assertEquals("server", serverProfile.target().name());
        assertEquals(List.of("nogui"), serverProfile.gameArgs());
        // and they pass the validation of the targets
        assertSame(clientProfile, client.resolve(clientProfile, new TypedMapImpl()));
        assertSame(serverProfile, server.resolve(serverProfile, new TypedMapImpl()));
        assertThrows(RuntimeException.class, () -> server.resolve(clientProfile, new TypedMapImpl()));
    }

    // completions

    private static List<String> values(List<ArgCandidate> candidates) {
        return candidates.stream().map(ArgCandidate::value).toList();
    }

    @Test
    void clientCompletesPlatformsAndVersions() {
        List<String> first = values(client.complete(TargetArgs.of("")));

        assertTrue(first.contains("fabric"), first.toString());
        // only platforms that have clients
        assertFalse(first.contains("paper"), first.toString());
        // a client does not start with "server", and "client" is optional
        assertFalse(first.contains("server"), first.toString());
        assertEquals(List.of("1.21.1", "1.20.4"), values(client.complete(TargetArgs.of("fabric", ""))));
        assertEquals(List.of("0.16.10", "0.16.9"), values(client.complete(TargetArgs.of("fabric", "1.21.1", ""))));
        assertTrue(values(client.complete(TargetArgs.of("1"))).contains("1.21.1"));
    }

    @Test
    void clientCompletesNothingForAServerVersion() {
        assertEquals(List.of(), client.complete(TargetArgs.of("server", "")));
    }

    @Test
    void serverCompletesTheKeywordFirst() {
        assertEquals(List.of("server"), values(server.complete(TargetArgs.of(""))));
        assertEquals(List.of(), server.complete(TargetArgs.of("fabric", "")));
        assertEquals(List.of("1.21.1", "1.20.4"), values(server.complete(TargetArgs.of("server", "fabric", ""))));
    }

    @Test
    void serverCompletesVersionsDirectlyForCommandsThatOnlyLaunchServers() {
        assertEquals(List.of("1.21.1", "1.20.4"), values(server.complete(serverOnly("fabric", ""))));
        List<String> first = values(server.complete(serverOnly("")));
        assertTrue(first.containsAll(List.of("fabric", "paper")), first.toString());
        assertFalse(first.contains("client"), first.toString());
    }

    // TargetArgs

    @Test
    void argsKnowWhatTheCommandAllows() {
        assertTrue(serverOnly().isServerOnly());
        assertFalse(serverOnly().isClientOnly());
        assertTrue(clientOnly().isClientOnly());
        assertFalse(TargetArgs.of().isServerOnly());
        assertTrue(TargetArgs.of().allows(Optional.of(Side.CLIENT)));
        assertTrue(TargetArgs.of().allows(Optional.empty()));
        assertFalse(serverOnly().allows(Optional.of(Side.CLIENT)));
        // something without a side is not a server
        assertFalse(serverOnly().allows(Optional.empty()));
        assertEquals("fabric 1.21.1", TargetArgs.of("fabric", "1.21.1").toString());
        assertEquals(serverOnly("x").sides(), serverOnly("x").withArgs(List.of("y")).sides());
    }

}

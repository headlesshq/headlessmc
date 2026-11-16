package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.launcher.profile.ArgCandidate;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.ParsedTarget;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.TargetArgs;
import io.github.headlesshq.headlessmc.launcher.profile.TargetInfo;
import io.github.headlesshq.headlessmc.launcher.profile.VersionHandle;
import io.github.headlesshq.headlessmc.util.typemap.Key;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static io.github.headlesshq.headlessmc.launcher.client.ClientTargetEnvironment.LATEST_BUILD;
import static io.github.headlesshq.headlessmc.launcher.client.ClientTargetEnvironment.OLD_BUILD;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The launch target for packwiz packs: the pack says which version is launched,
 * resolving and launching that version is done by the launch target for versions.
 */
class PackwizLaunchTargetTest {
    private static final Account ACCOUNT = new Account("msa", "Steve", "uuid", "token", Account.TYPE_MSA, "xuid");

    @TempDir
    Path root;

    private ClientTargetEnvironment env;
    private PackwizLaunchTarget target;
    private Path packDir;
    private Path packToml;

    @BeforeEach
    void setup() throws IOException {
        env = new ClientTargetEnvironment(root, new FakeProfileService(root.resolve("profiles")));
        target = env.packwiz;
        packDir = Files.createDirectories(root.resolve("packs").resolve("my-pack"));
        packToml = writePack("fabric = \"" + OLD_BUILD + "\"");
    }

    private Path writePack(String... loaders) throws IOException {
        return Files.writeString(
            packDir.resolve("pack.toml"),
            "name = \"My Pack\"\n[versions]\nminecraft = \"1.21.1\"\n" + String.join("\n", loaders) + "\n"
        );
    }

    private static <V> TypedMap options(Key<V> key, V value) {
        TypedMap options = new TypedMapImpl();
        options.put(key, value);
        return options;
    }

    private Profile profile(String... args) {
        return target.create("pack", target.parse(TargetArgs.of(args)), new TypedMapImpl());
    }

    private Profile client() {
        return profile(packToml.toString(), "client");
    }

    private VersionHandle inner(Profile profile) {
        return env.version.handle(Objects.requireNonNull(target.handle(profile).resolved()));
    }

    // arguments

    @Test
    void aPackIsParsedIntoTheVersionItAsksFor() {
        ParsedTarget parsed = target.parse(TargetArgs.of(packToml.toString(), "client"));

        PackwizLaunchTarget.Handle handle = target.handle(parsed.handle());
        assertEquals(PackwizLaunchTarget.NAME, parsed.handle().name());
        assertEquals(Optional.of(Side.CLIENT), parsed.side());
        assertEquals(packToml, handle.file());
        assertEquals("fabric", handle.platform());
        // the handle of the launch target for client versions, not resolved yet
        assertEquals(VersionHandle.VERSION, Objects.requireNonNull(handle.resolved()).name());
        assertEquals(
            VersionHandle.of(VersionArg.parse("fabric", "1.21.1", OLD_BUILD)), env.version.handle(handle.resolved())
        );
    }

    @Test
    void theDirectoryOfAPackCanBeGiven() {
        assertEquals(
            target.parse(TargetArgs.of(packToml.toString(), "client")),
            target.parse(TargetArgs.of(packDir.toString(), "client"))
        );
    }

    @Test
    void aPackNeedsASideUnlessTheCommandOnlyLaunchesOne() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> target.parse(TargetArgs.of(packToml.toString()))
        );
        assertTrue(e.getMessage().contains("please specify which"), e.getMessage());

        ParsedTarget client = target.parse(new TargetArgs(List.of(packToml.toString()), Set.of(Side.CLIENT)));
        ParsedTarget server = target.parse(new TargetArgs(List.of(packToml.toString()), Set.of(Side.SERVER)));

        assertEquals(Optional.of(Side.CLIENT), client.side());
        assertEquals(Optional.of(Side.SERVER), server.side());
        assertEquals(VersionHandle.SERVER, Objects.requireNonNull(target.handle(server.handle()).resolved()).name());
    }

    @Test
    void aPackCanBeLaunchedAsAServer() {
        ParsedTarget parsed = target.parse(TargetArgs.of(packToml.toString(), "server"));

        assertEquals(Optional.of(Side.SERVER), parsed.side());
        assertEquals(VersionHandle.SERVER, Objects.requireNonNull(target.handle(parsed.handle()).resolved()).name());
    }

    @Test
    void aVanillaPackIsParsed() throws IOException {
        writePack();

        Profile profile = client();

        assertEquals(VersionArg.parse("1.21.1"), inner(profile).version());
    }

    @Test
    void aPackWithMultipleLoadersNeedsALoader() throws IOException {
        writePack("fabric = \"" + OLD_BUILD + "\"", "paper = \"130\"");

        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> target.parse(TargetArgs.of(packToml.toString(), "client"))
        );
        assertTrue(e.getMessage().contains("supports multiple loaders, please specify one of fabric, paper"), e.getMessage());

        ParsedTarget fabric = target.parse(TargetArgs.of(packToml.toString(), "client", "fabric"));
        ParsedTarget paper = target.parse(TargetArgs.of(packToml.toString(), "paper", "server"));
        assertEquals("fabric", target.handle(fabric.handle()).platform());
        assertEquals("paper", target.handle(paper.handle()).platform());
        assertEquals(Optional.of(Side.SERVER), paper.side());

        // paper has no clients, the launch target for versions says so
        e = assertThrows(
            IllegalArgumentException.class, () -> target.parse(TargetArgs.of(packToml.toString(), "client", "paper"))
        );
        assertTrue(e.getMessage().contains("does not support clients"), e.getMessage());
        e = assertThrows(
            IllegalArgumentException.class, () -> target.parse(TargetArgs.of(packToml.toString(), "client", "forge"))
        );
        assertTrue(e.getMessage().contains("the pack has no loader forge, available: fabric, paper"), e.getMessage());
    }

    @Test
    void thingsThatAreNotAPackAreRejected() throws IOException {
        Path empty = Files.createDirectories(root.resolve("empty"));
        Path json = env.installCustom(env.primary, "my-pack");
        Path noVersion = Files.writeString(Files.createDirectories(root.resolve("broken")).resolve("pack.toml"), "name = \"x\"\n");
        Path unknownVersion = Files.writeString(
            Files.createDirectories(root.resolve("unknown")).resolve("pack.toml"), "[versions]\nminecraft = \"0.0.1\"\n"
        );

        for (Path path : List.of(root.resolve("missing"), empty, json, noVersion, unknownVersion)) {
            assertThrows(
                IllegalArgumentException.class, () -> target.parse(TargetArgs.of(path.toString(), "client")), path.toString()
            );
        }

        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of()));
        assertThrows(
            IllegalArgumentException.class, () -> target.parse(TargetArgs.of(packToml.toString(), "client", "fabric", "x"))
        );
        // a version is not a pack, and a pack is not a version
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of("fabric", "1.21.1")));
        assertThrows(IllegalArgumentException.class, () -> env.version.parse(TargetArgs.of(packToml.toString())));
    }

    @Test
    void theTargetIsDetectedForAPack() {
        assertSame(target, env.launchTargetService.parse(null, TargetArgs.of(packDir.toString(), "client")).target());
        assertSame(target, env.launchTargetService.parse(null, TargetArgs.of(packToml.toString(), "server")).target());
    }

    @Test
    void completesTheSidesTheCommandLaunches() {
        assertEquals(
            List.of(new ArgCandidate("client"), new ArgCandidate("server")),
            target.complete(TargetArgs.of(packToml.toString(), ""))
        );
        assertEquals(
            List.of(new ArgCandidate("server")),
            target.complete(new TargetArgs(List.of(packToml.toString(), ""), Set.of(Side.SERVER)))
        );
    }

    // profiles

    @Test
    void aPackIsPlayedInItsOwnDirectory() {
        Profile profile = client();

        assertEquals("pack", profile.name());
        assertEquals(Optional.of(Side.CLIENT), profile.side());
        assertEquals(packDir, profile.path());
        assertEquals(PackwizLaunchTarget.NAME, profile.target().name());
        assertEquals("packwiz:my-pack", target.getLabel(profile));
        // a pack has to be named by the user
        assertEquals(Optional.empty(), target.defaultName(profile.target()));
    }

    @Test
    void theGameDirectoryCanBeChosen() {
        Profile profile = target.create(
            "pack", target.parse(TargetArgs.of(packToml.toString(), "client")),
            options(LaunchTarget.GAME_DIR, root.resolve("elsewhere"))
        );

        assertEquals(root.resolve("elsewhere"), profile.path());
    }

    @Test
    void aServerProfileHasTheDefaultsOfAServer() {
        Profile profile = profile(packToml.toString(), "server");

        assertEquals(Optional.of(Side.SERVER), profile.side());
        assertEquals(List.of("nogui"), profile.gameArgs());
        assertEquals(packDir, profile.path());
    }

    @Test
    void aProfileIsAProfileForItsPackAndLoader() throws IOException {
        Profile profile = client();
        Path other = Files.createDirectories(root.resolve("packs").resolve("other"));
        Files.copy(packToml, other.resolve("pack.toml"));

        assertTrue(target.isSameTarget(profile, target.parse(TargetArgs.of(packDir.toString(), "client"))));
        assertFalse(target.isSameTarget(profile, target.parse(TargetArgs.of(other.toString(), "client"))));
        // also if the pack has been updated in the meantime
        writePack("fabric = \"" + LATEST_BUILD + "\"");
        assertTrue(target.isSameTarget(profile, target.parse(TargetArgs.of(packDir.toString(), "client"))));
    }

    @Test
    void describesTheVersionOfThePack() {
        Profile profile = client();

        TargetInfo before = target.describe(profile);
        Profile resolved = target.resolve(profile, new TypedMapImpl());
        TargetInfo after = target.describe(resolved);

        assertEquals(Optional.of(VersionArg.parse("fabric", "1.21.1", OLD_BUILD)), before.specified());
        assertEquals(Optional.empty(), before.file());
        assertEquals(Optional.of(env.json(env.primary, "fabric-loader-" + OLD_BUILD + "-1.21.1")), after.file());
    }

    // resolving

    @Test
    void resolvingInstallsAndPinsTheVersionOfThePack() {
        Profile profile = client();

        Profile resolved = target.resolve(profile, new TypedMapImpl());

        assertEquals(List.of(new ClientTargetEnvironment.Install(env.id("fabric", "1.21.1", OLD_BUILD), env.primary)), env.installs);
        // still a profile of the pack, with the pin of the version inside
        assertEquals(PackwizLaunchTarget.NAME, resolved.target().name());
        assertEquals(packToml, target.handle(resolved).file());
        assertEquals(env.json(env.primary, "fabric-loader-" + OLD_BUILD + "-1.21.1"), inner(resolved).currentTarget());
        assertEquals(packDir, resolved.path());
        assertEquals(Optional.of(Side.CLIENT), resolved.side());
    }

    @Test
    void aSecondResolveChangesNothing() {
        Profile resolved = target.resolve(client(), new TypedMapImpl());

        assertSame(resolved, target.resolve(resolved, new TypedMapImpl()));
        assertEquals(1, env.installs.size());
    }

    @Test
    void theOptionsOfTheResolveReachTheVersionTarget() {
        Profile resolved = target.resolve(client(), options(LaunchTarget.MC_FILES, env.secondary));

        assertEquals(env.secondary, env.installs.getFirst().mcFiles());
        assertEquals(env.json(env.secondary, "fabric-loader-" + OLD_BUILD + "-1.21.1"), inner(resolved).currentTarget());
    }

    @Test
    void aPackThatHasBeenUpdatedIsResolvedToItsNewVersion() throws IOException {
        Profile resolved = target.resolve(client(), new TypedMapImpl());
        // the pack is updated, e.g. by a git pull
        writePack("fabric = \"" + LATEST_BUILD + "\"");

        Profile updated = target.resolve(resolved, new TypedMapImpl());

        assertEquals(VersionArg.parse("fabric", "1.21.1", LATEST_BUILD), inner(updated).version());
        assertEquals(env.json(env.primary, "fabric-loader-" + LATEST_BUILD + "-1.21.1"), inner(updated).currentTarget());
        assertEquals(2, env.installs.size());
        // the pack decides, not the pin
        assertSame(updated, target.resolve(updated, new TypedMapImpl()));
    }

    @Test
    void aPackThatIsGoneOrBrokenIsReported() throws IOException {
        Profile resolved = target.resolve(client(), new TypedMapImpl());

        Files.writeString(packToml, "name = \"no versions\"\n");
        LaunchException broken = assertThrows(LaunchException.class, () -> target.resolve(resolved, new TypedMapImpl()));
        Files.delete(packToml);
        LaunchException gone = assertThrows(LaunchException.class, () -> target.resolve(resolved, new TypedMapImpl()));

        assertTrue(broken.getMessage().contains("Failed to read the pack of profile pack"), broken.getMessage());
        assertTrue(gone.getMessage().contains("of profile pack does not exist"), gone.getMessage());
    }

    @Test
    void aPackProfileWithoutASideIsRejected() {
        Profile sideless = client().withSide(Optional.empty());

        assertThrows(LaunchException.class, () -> target.resolve(sideless, new TypedMapImpl()));
        assertThrows(LaunchException.class, () -> target.launcher(sideless, options(ClientLauncher.ACCOUNT, ACCOUNT)));
    }

    // launching

    @Test
    void launchesTheVersionOfThePackInThePackDirectory() {
        TypedMap options = options(ClientLauncher.ACCOUNT, ACCOUNT);
        Profile resolved = target.resolve(client(), options);

        target.launcher(resolved, options);

        Mockito.verify(env.clientLauncher).launcher(
            Mockito.argThat(profile -> profile.path().equals(packDir) && profile.name().equals("pack")),
            Mockito.eq(ACCOUNT),
            Mockito.argThat(version -> version.getId().equals("fabric-loader-" + OLD_BUILD + "-1.21.1")),
            Mockito.eq(env.id("fabric", "1.21.1", OLD_BUILD))
        );
    }

    @Test
    void launchingDoesNotInstall() {
        assertThrows(LaunchException.class, () -> target.launcher(client(), options(ClientLauncher.ACCOUNT, ACCOUNT)));
        assertEquals(List.of(), env.installs);
    }

    // moving

    @Test
    void aPackInsideTheGameDirectoryMovesWithIt() {
        Profile resolved = target.resolve(client(), new TypedMapImpl());
        Path moved = root.resolve("moved");

        PackwizLaunchTarget.Handle handle = target.handle(target.relocate(resolved, moved));

        assertEquals(moved.resolve("pack.toml"), handle.file());
        // the version is installed in the .minecraft directory, it does not move
        assertEquals(inner(resolved), env.version.handle(Objects.requireNonNull(handle.resolved())));
    }

    @Test
    void aPackOutsideTheGameDirectoryStaysWhereItIs() {
        Profile profile = target.create(
            "pack", target.parse(TargetArgs.of(packToml.toString(), "client")),
            options(LaunchTarget.GAME_DIR, root.resolve("elsewhere"))
        );

        assertEquals(packToml, target.handle(target.relocate(profile, root.resolve("moved"))).file());
    }

    @Test
    void theHandleSurvivesBeingStored() {
        Profile resolved = target.resolve(client(), new TypedMapImpl());
        Path file = root.resolve("stored").resolve("profile.json");

        env.jsonService.write(file, resolved, true);
        Profile stored = env.jsonService.parse(file, Profile.class);

        assertEquals(target.handle(resolved).file(), target.handle(stored).file());
        assertEquals(inner(resolved), inner(stored));
        // and it resolves to the same thing, nothing is saved again
        assertSame(stored, target.resolve(stored, new TypedMapImpl()));
        assertEquals(1, env.installs.size());
    }

}

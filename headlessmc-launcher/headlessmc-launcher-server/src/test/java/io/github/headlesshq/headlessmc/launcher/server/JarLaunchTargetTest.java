package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.ArgCandidate;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.ParsedTarget;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.TargetArgs;
import io.github.headlesshq.headlessmc.launcher.profile.TargetInfo;
import io.github.headlesshq.headlessmc.launcher.profile.TestLaunchTargets;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import io.github.headlesshq.headlessmc.util.typemap.Key;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.arg.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.github.headlesshq.headlessmc.launcher.server.ExecutableTargetTestFiles.MAIN_CLASS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The launch target for jars, and with it what all targets for a single executable file have in common.
 */
class JarLaunchTargetTest {
    @TempDir
    Path root;

    private final ServerLauncher serverLauncher = Mockito.mock(ServerLauncher.class);
    private final FileService fileService = new DefaultFileService(new DefaultFileSystemProvider());
    private final JsonService jsonService = new DefaultJacksonJsonService();
    private JarLaunchTarget target;
    private ScriptLaunchTarget scripts;
    private Path jar;

    @BeforeEach
    void setup() {
        target = new JarLaunchTarget(serverLauncher, fileService, jsonService);
        scripts = new ScriptLaunchTarget(serverLauncher, fileService, jsonService);
        jar = ExecutableTargetTestFiles.jar(root.resolve("server").resolve("paper.jar"), MAIN_CLASS, 17);
    }

    private static <V> TypedMap options(Key<V> key, V value) {
        TypedMap options = new TypedMapImpl();
        options.put(key, value);
        return options;
    }

    private Profile profile(String... args) {
        return target.create("p", target.parse(TargetArgs.of(args)), new TypedMapImpl());
    }

    // arguments

    @Test
    void aJarIsParsed() {
        ParsedTarget parsed = target.parse(TargetArgs.of("server", jar.toString()));

        assertEquals(JarLaunchTarget.NAME, parsed.handle().name());
        assertEquals(new JarLaunchTarget.Handle(jar), target.handle(parsed.handle()));
        assertEquals(Optional.of(Side.SERVER), parsed.side());
    }

    @Test
    void thePathIsStoredAbsoluteAndNormalized() {
        Path notNormalized = jar.getParent().resolve("..").resolve("server").resolve("paper.jar");

        assertEquals(jar, target.handle(target.parse(TargetArgs.of(notNormalized.toString())).handle()).jar());
    }

    @Test
    void theSideOfAJarIsWhatTheUserSaysItIs() {
        // nothing tells what a jar is: without a side it has none
        assertEquals(Optional.empty(), target.parse(TargetArgs.of(jar.toString())).side());
        assertEquals(Optional.of(Side.CLIENT), target.parse(TargetArgs.of("client", jar.toString())).side());
        assertEquals(Optional.of(Side.SERVER), target.parse(TargetArgs.of("server", jar.toString())).side());
        // "server add ./paper.jar" adds a server
        assertEquals(
            Optional.of(Side.SERVER), target.parse(new TargetArgs(List.of(jar.toString()), Set.of(Side.SERVER))).side()
        );
        assertEquals(
            Optional.of(Side.CLIENT), target.parse(new TargetArgs(List.of(jar.toString()), Set.of(Side.CLIENT))).side()
        );
    }

    @Test
    void filesThatCannotBeLaunchedWithJavaAreRejected() {
        Path noMainClass = ExecutableTargetTestFiles.jar(root.resolve("library.jar"), null, null);
        Path notAZip = ExecutableTargetTestFiles.file(root.resolve("broken.jar"), "not a jar");
        Path script = ExecutableTargetTestFiles.file(root.resolve("run.sh"), "#!/bin/sh");

        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> target.parse(TargetArgs.of(noMainClass.toString()))
        );
        assertTrue(e.getMessage().contains("has no Main-Class"), e.getMessage());
        e = assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of(notAZip.toString())));
        assertTrue(e.getMessage().contains("is not a jar"), e.getMessage());
        e = assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of(script.toString())));
        assertTrue(e.getMessage().contains("is not a .jar file"), e.getMessage());
        e = assertThrows(
            IllegalArgumentException.class, () -> target.parse(TargetArgs.of(root.resolve("missing.jar").toString()))
        );
        assertTrue(e.getMessage().contains("does not exist"), e.getMessage());
        // a directory called like a jar
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of(root.resolve("server").toString())));
    }

    @Test
    void otherArgumentsAreRejected() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> target.parse(TargetArgs.of("paper", jar.toString()))
        );
        assertTrue(e.getMessage().contains("'paper' is neither 'client' nor 'server'"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of()));
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of("server", jar.toString(), "x")));
        // a version is not a jar
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of("paper", "1.21.1")));
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of("1.21.1")));
    }

    @Test
    void jarsAndScriptsAreDetectedByTheirFile() {
        Path script = ExecutableTargetTestFiles.file(root.resolve("server").resolve("run.sh"), "#!/bin/sh");
        LaunchTargetService service = new TestLaunchTargets(new FakeProfileService(root.resolve("profiles")))
            .with(target, scripts)
            .service();

        // exactly one of the targets can launch each of the files
        assertSame(target, service.parse(null, TargetArgs.of("server", jar.toString())).target());
        assertSame(scripts, service.parse(null, TargetArgs.of("server", script.toString())).target());
        assertSame(target, service.parse(null, TargetArgs.of(jar.toString())).target());
        assertThrows(IllegalArgumentException.class, () -> service.parse(null, TargetArgs.of("paper", "1.21.1")));
    }

    @Test
    void completesSidesAndJars() {
        ExecutableTargetTestFiles.file(root.resolve("server").resolve("run.sh"), "#!/bin/sh");
        String dir = jar.getParent() + "/";

        List<ArgCandidate> first = target.complete(TargetArgs.of(dir));
        List<ArgCandidate> afterSide = target.complete(TargetArgs.of("server", dir));

        assertEquals(
            List.of(new ArgCandidate("client"), new ArgCandidate("server"), new ArgCandidate(dir + "paper.jar")), first
        );
        assertEquals(List.of(new ArgCandidate(dir + "paper.jar")), afterSide);
        // the commands for servers do not offer clients
        assertEquals(
            List.of(new ArgCandidate("server"), new ArgCandidate(dir + "paper.jar")),
            target.complete(new TargetArgs(List.of(dir), Set.of(Side.SERVER)))
        );
        assertEquals(List.of(), target.complete(TargetArgs.of("paper", dir)));
        assertEquals(List.of(), target.complete(TargetArgs.of("server", dir, "")));
    }

    // profiles

    @Test
    void aJarIsLaunchedInItsOwnDirectory() {
        Profile profile = profile("server", jar.toString());

        assertEquals("p", profile.name());
        assertEquals(Optional.of(Side.SERVER), profile.side());
        assertEquals(jar.getParent(), profile.path());
        assertEquals(List.of(), profile.gameArgs());
        assertNull(profile.javaVersion());
        assertEquals("jar:paper.jar", target.getLabel(profile));
        assertEquals(new TargetInfo(Optional.empty(), Optional.empty(), Optional.of(jar)), target.describe(profile));
        // a file does not make a good name, the user has to name the profile
        assertEquals(Optional.empty(), target.defaultName(profile.target()));
    }

    @Test
    void aProfileWithoutASideStaysWithoutASide() {
        Profile profile = profile(jar.toString());

        assertEquals(Optional.empty(), profile.side());
        assertFalse(profile.isClient());
        assertFalse(profile.isServer());
        assertEquals(Optional.empty(), target.resolve(profile, new TypedMapImpl()).side());
    }

    @Test
    void theGameDirectoryCanBeChosen() {
        Profile profile = target.create(
            "p", target.parse(TargetArgs.of(jar.toString())), options(LaunchTarget.GAME_DIR, root.resolve("world"))
        );

        assertEquals(root.resolve("world"), profile.path());
        assertEquals(jar, target.handle(profile).jar());
    }

    @Test
    void aProfileIsAProfileForItsJar() {
        Profile profile = profile("server", jar.toString());
        Path other = ExecutableTargetTestFiles.jar(root.resolve("other.jar"), MAIN_CLASS, 17);

        assertTrue(target.isSameTarget(profile, target.parse(TargetArgs.of(jar.toString()))));
        assertFalse(target.isSameTarget(profile, target.parse(TargetArgs.of(other.toString()))));
    }

    // resolving

    @Test
    void aProfileGetsTheJavaVersionTheJarHasBeenCompiledFor() {
        Profile resolved = target.resolve(profile("server", jar.toString()), new TypedMapImpl());

        assertEquals(17, resolved.javaVersion());
        assertEquals(profile("server", jar.toString()), resolved.withJavaVersion(null));
        // resolved once, it does not change anymore
        assertSame(resolved, target.resolve(resolved, new TypedMapImpl()));
    }

    @Test
    void theJavaVersionOfAProfileIsKept() {
        Profile profile = profile("server", jar.toString()).withJavaVersion(21);

        assertSame(profile, target.resolve(profile, new TypedMapImpl()));
        assertSame(profile, target.resolve(profile, options(LaunchTarget.UPDATE, true)));
        assertSame(profile, target.resolve(profile, options(LaunchTarget.NEW_PROFILE, true)));
    }

    @Test
    void veryOldJarsAreLaunchedWithJava8() {
        Path old = ExecutableTargetTestFiles.jar(root.resolve("old.jar"), MAIN_CLASS, 5);

        assertEquals(8, target.resolve(profile(old.toString()), new TypedMapImpl()).javaVersion());
    }

    @Test
    void aJarWhoseMainClassCannotBeReadIsLaunchedWithTheCurrentJava() {
        // e.g. the main class is in another jar on the Class-Path of the manifest
        Path launcher = ExecutableTargetTestFiles.jar(root.resolve("launcher.jar"), MAIN_CLASS, null);

        assertEquals(
            Runtime.version().feature(), target.resolve(profile(launcher.toString()), new TypedMapImpl()).javaVersion()
        );
    }

    @Test
    void aJarThatHasBeenRemovedIsReported() throws IOException {
        Profile profile = profile("server", jar.toString());
        Files.delete(jar);

        LaunchException resolve = assertThrows(LaunchException.class, () -> target.resolve(profile, new TypedMapImpl()));
        LaunchException launch = assertThrows(LaunchException.class, () -> target.launcher(profile, new TypedMapImpl()));

        assertEquals("The jar " + jar + " of profile p does not exist.", resolve.getMessage());
        assertEquals(resolve.getMessage(), launch.getMessage());
        // but the profile can still be listed
        assertEquals(Optional.of(jar), target.describe(profile).file());
    }

    // launching

    @Test
    void launchesTheJar() {
        Profile profile = profile("server", jar.toString()).withJavaVersion(17);
        TypedMap options = options(ServerLauncher.PROCESS_NAME, "p");
        ProcessLauncher processLauncher = Mockito.mock(ProcessLauncher.class);
        Mockito.when(serverLauncher.launcher(profile, jar, options)).thenReturn(processLauncher);

        assertSame(processLauncher, target.launcher(profile, options));
    }

    // moving

    @Test
    void aJarInsideTheGameDirectoryMovesWithIt() {
        Profile profile = profile("server", jar.toString());
        Path moved = root.resolve("moved");

        assertEquals(moved.resolve("paper.jar"), target.handle(target.relocate(profile, moved)).jar());
    }

    @Test
    void aJarOutsideTheGameDirectoryStaysWhereItIs() {
        Profile profile = target.create(
            "p", target.parse(TargetArgs.of(jar.toString())), options(LaunchTarget.GAME_DIR, root.resolve("world"))
        );

        assertEquals(jar, target.handle(target.relocate(profile, root.resolve("moved"))).jar());
    }

    @Test
    void theHandleSurvivesBeingStored() {
        Profile profile = profile(jar.toString());
        Path file = root.resolve("stored").resolve("profile.json");

        jsonService.write(file, profile, true);
        Profile stored = jsonService.parse(file, Profile.class);

        assertEquals(Optional.empty(), stored.side());
        assertEquals(new JarLaunchTarget.Handle(jar), target.handle(stored));
        assertTrue(target.isSameTarget(stored, target.parse(TargetArgs.of(jar.toString()))));
    }

    @Test
    void aHandleWithoutItsFileIsInvalid() throws IOException {
        Path file = root.resolve("stored").resolve("profile.json");
        jsonService.write(file, profile(jar.toString()), true);
        // edited by hand
        Files.writeString(file, Files.readString(file).replace("\"jar\"", "\"file\""));
        Profile broken = jsonService.parse(file, Profile.class);

        // the profile can be read, e.g. to be listed and repaired, it just cannot be launched
        LaunchException e = assertThrows(LaunchException.class, () -> target.resolve(broken, new TypedMapImpl()));
        assertTrue(e.getMessage().contains("Profile p is invalid"), e.getMessage());
    }

}

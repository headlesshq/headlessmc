package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.ArgCandidate;
import io.github.headlesshq.headlessmc.launcher.profile.ParsedTarget;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.TargetArgs;
import io.github.headlesshq.headlessmc.launcher.profile.TargetInfo;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.arg.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The launch target for scripts. What it has in common with the target for jars is tested by {@link JarLaunchTargetTest}.
 */
class ScriptLaunchTargetTest {
    @TempDir
    Path root;

    private final ServerLauncher serverLauncher = Mockito.mock(ServerLauncher.class);
    private final JsonService jsonService = new DefaultJacksonJsonService();
    private ScriptLaunchTarget target;
    private Path script;

    @BeforeEach
    void setup() {
        target = new ScriptLaunchTarget(serverLauncher, new DefaultFileService(new DefaultFileSystemProvider()), jsonService);
        script = ExecutableTargetTestFiles.file(root.resolve("server").resolve("run.sh"), "#!/bin/sh\njava @args.txt\n");
    }

    private Profile profile(String... args) {
        return target.create("p", target.parse(TargetArgs.of(args)), new TypedMapImpl());
    }

    private static Path executable(Path file) throws IOException {
        return Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwxr-xr-x"));
    }

    @Test
    void aScriptIsParsed() {
        ParsedTarget parsed = target.parse(TargetArgs.of("server", script.toString()));

        assertEquals(ScriptLaunchTarget.NAME, parsed.handle().name());
        assertEquals(new ScriptLaunchTarget.Handle(script), target.handle(parsed.handle()));
        assertEquals(Optional.of(Side.SERVER), parsed.side());
        // without a side it has none, unless the command only launches servers
        assertEquals(Optional.empty(), target.parse(TargetArgs.of(script.toString())).side());
        assertEquals(
            Optional.of(Side.SERVER),
            target.parse(new TargetArgs(List.of(script.toString()), Set.of(Side.SERVER))).side()
        );
    }

    @Test
    void scriptsOfAllSystemsAreRecognizedByTheirExtension() {
        for (String name : List.of("run.sh", "run.bat", "start.cmd", "Start.PS1")) {
            Path file = ExecutableTargetTestFiles.file(root.resolve(name), "echo");

            assertEquals(file, target.handle(target.parse(TargetArgs.of(file.toString())).handle()).script(), name);
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void anExecutableFileIsAScript() throws IOException {
        Path start = executable(ExecutableTargetTestFiles.file(root.resolve("start"), "#!/bin/sh"));

        assertEquals(start, target.handle(target.parse(TargetArgs.of(start.toString())).handle()).script());
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void filesOfOtherTargetsAreNotScriptsEvenIfTheyAreExecutable() throws IOException {
        for (String name : List.of("server.jar", "version.json", "pack.toml")) {
            Path file = executable(ExecutableTargetTestFiles.file(root.resolve(name), "x"));

            IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class, () -> target.parse(TargetArgs.of(file.toString())), name
            );
            assertTrue(e.getMessage().contains("is not a script"), e.getMessage());
        }
    }

    @Test
    void otherFilesAreRejected() {
        Path text = ExecutableTargetTestFiles.file(root.resolve("notes.txt"), "notes");

        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of(text.toString())));
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of(root.resolve("missing.sh").toString())));
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of(root.resolve("server").toString())));
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of("run", script.toString())));
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of()));
    }

    @Test
    void completesScripts() {
        ExecutableTargetTestFiles.jar(root.resolve("server").resolve("server.jar"), "test.Main", 17);
        ExecutableTargetTestFiles.file(root.resolve("server").resolve("eula.txt"), "eula=true");
        String dir = script.getParent() + "/";

        assertEquals(List.of(new ArgCandidate(dir + "run.sh")), target.complete(TargetArgs.of("server", dir)));
    }

    @Test
    void aScriptIsRunInItsOwnDirectory() {
        Profile profile = profile("server", script.toString());

        assertEquals(script.getParent(), profile.path());
        assertEquals(Optional.of(Side.SERVER), profile.side());
        assertEquals("script:run.sh", target.getLabel(profile));
        assertEquals(new TargetInfo(Optional.empty(), Optional.empty(), Optional.of(script)), target.describe(profile));
    }

    @Test
    void aProfileWithoutAJavaVersionGetsTheCurrentJava() {
        // a script does not say which java it needs
        Profile resolved = target.resolve(profile("server", script.toString()), new TypedMapImpl());

        assertEquals(Runtime.version().feature(), resolved.javaVersion());
        assertSame(resolved, target.resolve(resolved, new TypedMapImpl()));
        Profile withJava = profile("server", script.toString()).withJavaVersion(8);
        assertSame(withJava, target.resolve(withJava, new TypedMapImpl()));
    }

    @Test
    void launchesTheScript() {
        Profile profile = profile("server", script.toString()).withJavaVersion(21);
        TypedMap options = new TypedMapImpl();
        ProcessLauncher processLauncher = Mockito.mock(ProcessLauncher.class);
        Mockito.when(serverLauncher.launcher(profile, script, options)).thenReturn(processLauncher);

        assertSame(processLauncher, target.launcher(profile, options));
    }

    @Test
    void aScriptThatHasBeenRemovedIsReported() throws IOException {
        Profile profile = profile("server", script.toString());
        Files.delete(script);

        LaunchException e = assertThrows(LaunchException.class, () -> target.launcher(profile, new TypedMapImpl()));

        assertEquals("The script " + script + " of profile p does not exist.", e.getMessage());
    }

    @Test
    void theScriptMovesWithTheGameDirectory() {
        Profile profile = profile("server", script.toString());

        assertEquals(
            root.resolve("moved").resolve("run.sh"),
            target.handle(target.relocate(profile, root.resolve("moved"))).script()
        );
    }

    @Test
    void theHandleSurvivesBeingStored() {
        Profile profile = profile("server", script.toString());
        Path file = root.resolve("stored").resolve("profile.json");

        jsonService.write(file, profile, true);
        Profile stored = jsonService.parse(file, Profile.class);

        assertEquals(new ScriptLaunchTarget.Handle(script), target.handle(stored));
        assertEquals(Optional.of(Side.SERVER), stored.side());
    }

}

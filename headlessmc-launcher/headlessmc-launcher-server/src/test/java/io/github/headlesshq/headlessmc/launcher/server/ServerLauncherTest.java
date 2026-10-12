package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.SafePath;
import io.github.headlesshq.headlessmc.java.launcher.AbstractJavaProcessBuilder;
import io.github.headlesshq.headlessmc.java.launcher.JavaFinder;
import io.github.headlesshq.headlessmc.java.launcher.JavaLauncherService;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcess;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessBuilder;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessImpl;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.VanillaInstaller;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.server.ServerInstaller;
import io.github.headlesshq.headlessmc.platform.server.ServerSupport;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import io.github.headlesshq.headlessmc.os.OS;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ServerLauncherTest {
    @TempDir
    Path root;

    /** Exposes the built command instead of starting a process. */
    private final class RecordingBuilder extends AbstractJavaProcessBuilder {
        @Override
        public JavaProcess start() {
            return new JavaProcessImpl(
                getId(), Map.of(), List.of(), List.of(), List.of(),
                Optional.ofNullable(getJava()), Optional.empty(), Optional.empty(),
                Optional.ofNullable(getDirectory()), Optional.ofNullable(getJar()), Optional.empty()
            );
        }
    }

    private final List<Integer> requestedJavaVersions = new java.util.ArrayList<>();

    private final JavaLauncherService launcherService = new JavaLauncherService() {
        @Override
        public JavaProcessBuilder buildProcess() {
            return new RecordingBuilder();
        }

        @Override
        public String getName() {
            return "recording";
        }
    };

    private final JavaFinder javaFinder = Mockito.mock(JavaFinder.class);

    private final VanillaInstaller vanillaInstaller = new VanillaInstaller() {
        @Override
        public Version getVersion(VanillaVersion version) {
            return new FakeVersion(version.getName()).withJavaVersion(21);
        }

        @Override
        public Version installClient(VersionID id, Path mcDir, TypedMap args) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Installation installServer(VersionID id, Path dir, TypedMap args) {
            throw new UnsupportedOperationException();
        }
    };

    private boolean mockJava = false;

    private final ScriptConfig scriptConfig = new ScriptConfig() {
        @Override
        public boolean mockJava() {
            return mockJava;
        }

        @Override
        public int timeout() {
            return 30;
        }
    };

    private final ScriptLauncher scriptLauncher = new ScriptLauncher(
        () -> scriptConfig, new OS("linux", OS.Type.LINUX, "1")
    );

    private FakePlatformService platformService;
    private FakeProfileService profileService;
    private ServerLauncher launcher;

    @BeforeEach
    void setup() {
        Mockito.when(javaFinder.findJava(Mockito.anyInt())).thenAnswer(invocation -> {
            int version = invocation.getArgument(0);
            requestedJavaVersions.add(version);
            Path home = root.resolve("java-" + version);
            return new Java("java-" + version, version, new SafePath(home),
                new SafePath(home.resolve("bin/java")), false, 0);
        });

        FakeVanillaPlatform vanilla = new FakeVanillaPlatform("1.21.1");
        vanilla.withServerSupport(serverSupport("server.jar"));
        platformService = new FakePlatformService(vanilla);
        profileService = new FakeProfileService(root);
        launcher = new ServerLauncher(
            launcherService, vanillaInstaller, platformService, profileService, new FakeServerService(profileService), javaFinder, scriptLauncher
        );
    }

    private ServerSupport serverSupport(String jarName) {
        return new ServerSupport(
            dir -> dir.resolve(jarName),
            (id, dir, args) -> new ServerInstaller.Installation(21)
        );
    }

    private RecordingBuilder builder(ProcessLauncher process) {
        return (RecordingBuilder) process.getJavaProcessBuilder().orElseThrow();
    }

    private Profile server(String name) {
        return new Profile(name, VersionArg.parse("server", "vanilla", "1.21.1"), root.resolve(name));
    }

    @Test
    void javaVersionOfAProfileIsKept() {
        Profile profile = server("srv").withJavaVersion(17);

        assertSame(profile, launcher.findJavaVersion(profile));
    }

    @Test
    void javaVersionIsTakenFromAnotherProfileOfTheSameVersion() {
        profileService.save(server("other").withJavaVersion(11));

        assertEquals(11, launcher.findJavaVersion(server("srv")).javaVersion());
    }

    @Test
    void javaVersionFallsBackToTheVanillaVersionJson() {
        assertEquals(21, launcher.findJavaVersion(server("srv")).javaVersion());
    }

    @Test
    void launcherBuildsAJavaProcessForTheServerJar() {
        Profile profile = server("srv").withJavaVersion(17)
            .withVmArgs(List.of("-Xmx2G"))
            .withGameArgs(List.of("nogui"));

        ProcessLauncher process = launcher.launcher(profile);

        assertEquals("mc-server-srv", process.getId());
        assertEquals(root.resolve("srv"), process.getGameDir());
        RecordingBuilder builder = builder(process);
        assertEquals(root.resolve("srv").resolve("server.jar"), builder.getJar());
        assertEquals(root.resolve("srv"), builder.getDirectory());
        assertEquals(List.of("-Xmx2G"), builder.getJvmArgs());
        assertEquals(List.of("nogui"), builder.getArgs());
        assertEquals(List.of(17), requestedJavaVersions);
        assertFalse(builder.isPipeIO());
    }

    @Test
    void launcherProcessCanPipeIo() {
        ProcessLauncher process = launcher.launchProcess(server("srv").withJavaVersion(17), "custom-id", true);

        assertTrue(builder(process).isPipeIO());
        assertEquals("custom-id", process.getId());
        assertEquals("custom-id", builder(process).getId());
    }

    @Test
    void profilesWithoutGameArgsAreSupported() {
        ProcessLauncher process = launcher.launchProcess(server("srv").withJavaVersion(17).withGameArgs(null), "id", false);

        assertEquals(List.of(), builder(process).getArgs());
    }

    private ServerLauncher scriptServerLauncher() {
        FakeVanillaPlatform vanilla = new FakeVanillaPlatform("1.21.1");
        vanilla.withServerSupport(serverSupport("run.sh"));
        return new ServerLauncher(
            launcherService, vanillaInstaller, new FakePlatformService(vanilla), profileService,
            new FakeServerService(profileService), javaFinder, scriptLauncher
        );
    }

    private Path writeForgeLikeServer(String name) throws IOException {
        Path dir = root.resolve(name);
        Path libraries = dir.resolve("libraries/net/minecraftforge/forge/1.21.1-52.0.0");
        Files.createDirectories(libraries);
        Files.writeString(dir.resolve("run.sh"), """
            #!/usr/bin/env sh
            # Forge requires a configured set of both JVM and program arguments.
            java @user_jvm_args.txt @libraries/net/minecraftforge/forge/1.21.1-52.0.0/unix_args.txt "$@"
            """);
        Files.writeString(dir.resolve("user_jvm_args.txt"), """
            # Xmx and Xms set the maximum and minimum RAM usage, respectively.
            -Xmx4G
            """);
        Files.writeString(libraries.resolve("unix_args.txt"), """
            -DlibraryDirectory=libraries
            -p libraries/a.jar:libraries/b.jar
            --add-modules ALL-MODULE-PATH
            -cp libraries/c.jar:libraries/d.jar
            cpw.mods.bootstraplauncher.BootstrapLauncher
            --launchTarget forgeserver
            "--fml.forgeVersion" 52.0.0
            """);
        return dir;
    }

    @Test
    @DisabledOnOs(org.junit.jupiter.api.condition.OS.WINDOWS)
    void scriptsAreLaunchedWithTheCapturedJavaCommand() throws IOException {
        mockJava = true;
        writeForgeLikeServer("srv");
        Profile profile = server("srv").withJavaVersion(21)
            .withVmArgs(List.of("-Xmx2G"))
            .withGameArgs(List.of("nogui"));

        ProcessLauncher process = scriptServerLauncher().launcher(profile);

        RecordingBuilder builder = builder(process);
        assertEquals("cpw.mods.bootstraplauncher.BootstrapLauncher", builder.getMainClass());
        assertNull(builder.getJar());
        assertEquals(List.of("libraries/c.jar", "libraries/d.jar"), builder.getClassPath());
        assertEquals(List.of(
            "-Xmx4G", "-DlibraryDirectory=libraries", "-p", "libraries/a.jar:libraries/b.jar",
            "--add-modules", "ALL-MODULE-PATH", "-Xmx2G"
        ), builder.getJvmArgs());
        assertEquals(List.of("--launchTarget", "forgeserver", "--fml.forgeVersion", "52.0.0", "nogui"), builder.getArgs());
        assertEquals(root.resolve("srv"), builder.getDirectory());
        assertEquals("java-21", Objects.requireNonNull(builder.getJava()).name());
    }

    @Test
    @DisabledOnOs(org.junit.jupiter.api.condition.OS.WINDOWS)
    void scriptsThatDoNotCallJavaFail() throws IOException {
        mockJava = true;
        Path dir = root.resolve("srv");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("run.sh"), "echo no java here\nexit 3\n");

        ServerException e = assertThrows(
            ServerException.class,
            () -> scriptServerLauncher().launcher(server("srv").withJavaVersion(17))
        );
        assertTrue(e.getMessage().contains("no java here"), e.getMessage());
    }

    @Test
    void scriptsAreLaunchedDirectlyByDefault() throws IOException {
        writeForgeLikeServer("srv");
        Profile profile = server("srv").withJavaVersion(21)
            .withVmArgs(List.of("-Xmx2G", "-Dname=with space"))
            .withGameArgs(List.of("nogui"));

        ProcessLauncher process = scriptServerLauncher().launcher(profile);

        assertTrue(process.getJavaProcessBuilder().isEmpty());
        ProcessBuilder processBuilder = process.getProcessBuilder().orElseThrow();
        assertEquals(List.of("sh", root.resolve("srv/run.sh").toAbsolutePath().toString(), "nogui"),
            processBuilder.command());
        assertTrue(processBuilder.environment().get("PATH").startsWith(root.resolve("java-21/bin").toString()));
        assertEquals("-Xmx2G \"-Dname=with space\"", processBuilder.environment().get("JDK_JAVA_OPTIONS"));
    }

    @Test
    void platformsWithoutServerSupportThrow() {
        FakePlatform fabric = FakePlatform.create("fabric", new String[]{"1.21.1", "0.16.9"});
        ServerLauncher unsupported = new ServerLauncher(
            launcherService, vanillaInstaller,
            new FakePlatformService(new FakeVanillaPlatform("1.21.1"), fabric),
            profileService, new FakeServerService(profileService), javaFinder, scriptLauncher
        );

        Profile profile = new Profile("srv", VersionArg.parse("server", "fabric", "1.21.1"), root.resolve("srv"))
            .withJavaVersion(17);

        assertThrows(IllegalStateException.class, () -> unsupported.launcher(profile));
    }

    @Test
    void eulaLauncherIsCreatedForThisLauncher() {
        assertNotNull(launcher.getEulaLauncher());
    }

}

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
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
            launcherService, vanillaInstaller, platformService, profileService, new FakeServerService(profileService), javaFinder
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

    @Test
    void nonJarExecutablesAreNotSupportedYet() {
        FakeVanillaPlatform vanilla = new FakeVanillaPlatform("1.21.1");
        vanilla.withServerSupport(serverSupport("start.sh"));
        ServerLauncher scriptLauncher = new ServerLauncher(
            launcherService, vanillaInstaller, new FakePlatformService(vanilla), profileService,
            new FakeServerService(profileService), javaFinder
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> scriptLauncher.launcher(server("srv").withJavaVersion(17))
        );
    }

    @Test
    void platformsWithoutServerSupportThrow() {
        FakePlatform fabric = FakePlatform.create("fabric", new String[]{"1.21.1", "0.16.9"});
        ServerLauncher unsupported = new ServerLauncher(
            launcherService, vanillaInstaller,
            new FakePlatformService(new FakeVanillaPlatform("1.21.1"), fabric),
            profileService, new FakeServerService(profileService), javaFinder
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

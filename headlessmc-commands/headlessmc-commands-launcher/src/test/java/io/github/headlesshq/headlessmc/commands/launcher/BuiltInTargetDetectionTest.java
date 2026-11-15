package io.github.headlesshq.headlessmc.commands.launcher;

import io.github.headlesshq.headlessmc.launcher.client.ClientTargetEnvironment;
import io.github.headlesshq.headlessmc.launcher.client.PackwizLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.client.VersionFileLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.TargetArgs;
import io.github.headlesshq.headlessmc.launcher.profile.VersionHandle;
import io.github.headlesshq.headlessmc.launcher.server.JarLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.server.ScriptLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.server.ServerLauncher;
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
import java.util.Optional;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * All launch targets HeadlessMc comes with, together: for everything that can be launched,
 * exactly the target that is meant has to be detected.
 * The targets for versions of servers and for client versions accept every version of the platforms here.
 */
class BuiltInTargetDetectionTest {
    @TempDir
    Path root;

    private ClientTargetEnvironment env;
    private LaunchTargetService service;
    private Path json;
    private Path jar;
    private Path script;
    private Path pack;

    @BeforeEach
    void setup() throws IOException {
        FakeProfileService profileService = new FakeProfileService(root.resolve("profiles"));
        env = new ClientTargetEnvironment(root, profileService);
        ServerLauncher serverLauncher = Mockito.mock(ServerLauncher.class);
        env.targets.with(
            new JarLaunchTarget(serverLauncher, env.fileService, env.jsonService),
            new ScriptLaunchTarget(serverLauncher, env.fileService, env.jsonService)
        );
        service = env.launchTargetService;

        json = env.installCustom(env.secondary, "my-pack");
        Path files = Files.createDirectories(root.resolve("files"));
        jar = files.resolve("paper.jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "test.Main");
        new JarOutputStream(Files.newOutputStream(jar), manifest).close();
        script = Files.writeString(files.resolve("run.sh"), "#!/bin/sh");
        pack = Files.createDirectories(root.resolve("packs").resolve("pack"));
        Files.writeString(pack.resolve("pack.toml"), "[versions]\nminecraft = \"1.21.1\"\nfabric = \"0.16.9\"\n");
        profileService.save(new Profile("main", VersionArg.parse("fabric", "1.21.1"), root.resolve("main")));
    }

    private String detect(String... args) {
        return service.parse(null, TargetArgs.of(args)).target().getName();
    }

    private String detect(Side side, String... args) {
        return service.parse(null, new TargetArgs(List.of(args), Set.of(side))).target().getName();
    }

    private IllegalArgumentException undetected(String... args) {
        return assertThrows(IllegalArgumentException.class, () -> detect(args), String.join(" ", args));
    }

    @Test
    void theTargetsThatTakePartInDetection() {
        assertEquals(
            Set.of(
                VersionHandle.VERSION, VersionHandle.SERVER, VersionFileLaunchTarget.NAME, PackwizLaunchTarget.NAME,
                JarLaunchTarget.NAME, ScriptLaunchTarget.NAME
            ),
            Set.copyOf(service.getTargets().stream().filter(LaunchTarget::isDetectable).map(LaunchTarget::getName).toList())
        );
        // a profile is only launched by another profile if that is asked for
        assertFalse(service.require(ProfileLaunchTarget.NAME).isDetectable());
    }

    @Test
    void versions() {
        assertEquals(VersionHandle.VERSION, detect("1.21.1"));
        assertEquals(VersionHandle.VERSION, detect("fabric", "1.21.1"));
        assertEquals(VersionHandle.VERSION, detect("fabric", "1.21.1", "0.16.9"));
        assertEquals(VersionHandle.VERSION, detect("client", "fabric", "1.21.1"));
        assertEquals(VersionHandle.VERSION, detect("fabric", "1.21.1", "latest"));
    }

    @Test
    void servers() {
        assertEquals(VersionHandle.SERVER, detect("server", "paper", "1.21.1"));
        assertEquals(VersionHandle.SERVER, detect("server", "1.21.1"));
        assertEquals(VersionHandle.SERVER, detect("server", "fabric", "1.21.1"));
        // the commands for servers do not need the "server"
        assertEquals(VersionHandle.SERVER, detect(Side.SERVER, "paper", "1.21.1"));
        assertEquals(VersionHandle.SERVER, detect(Side.SERVER, "1.21.1"));
        // everywhere else a version without it is a client, and paper has no clients
        undetected("paper", "1.21.1");
    }

    @Test
    void versionJsons() {
        assertEquals(VersionFileLaunchTarget.NAME, detect(json.toString()));
        // by the id of the installed version
        assertEquals(VersionFileLaunchTarget.NAME, detect("my-pack"));
        Path fabric = env.install(env.primary, "fabric", "1.21.1", "0.16.9");
        assertEquals(VersionFileLaunchTarget.NAME, detect(fabric.toString()));
        assertEquals(VersionFileLaunchTarget.NAME, detect("fabric-loader-0.16.9-1.21.1"));
        // an id that is a version is that version
        assertEquals(VersionHandle.VERSION, detect("1.21.1"));
        // a version.json is a client
        assertThrows(IllegalArgumentException.class, () -> detect(Side.SERVER, json.toString()));
        assertThrows(IllegalArgumentException.class, () -> detect(Side.SERVER, "my-pack"));
    }

    @Test
    void jarsAndScripts() {
        assertEquals(JarLaunchTarget.NAME, detect(jar.toString()));
        assertEquals(JarLaunchTarget.NAME, detect("server", jar.toString()));
        assertEquals(JarLaunchTarget.NAME, detect("client", jar.toString()));
        assertEquals(JarLaunchTarget.NAME, detect(Side.SERVER, jar.toString()));
        assertEquals(ScriptLaunchTarget.NAME, detect(script.toString()));
        assertEquals(ScriptLaunchTarget.NAME, detect("server", script.toString()));
        // without a side they have none
        assertEquals(Optional.empty(), service.parse(null, TargetArgs.of(jar.toString())).parsed().side());
        assertEquals(
            Optional.of(Side.SERVER), service.parse(null, TargetArgs.of("server", script.toString())).parsed().side()
        );
    }

    @Test
    void packs() {
        assertEquals(PackwizLaunchTarget.NAME, detect(pack.toString(), "client"));
        assertEquals(PackwizLaunchTarget.NAME, detect(pack.resolve("pack.toml").toString(), "server"));
        assertEquals(PackwizLaunchTarget.NAME, detect(pack.toString(), "client", "fabric"));
        assertEquals(PackwizLaunchTarget.NAME, detect(Side.SERVER, pack.toString()));
        // a pack can be both, the side has to be given
        IllegalArgumentException e = undetected(pack.toString());
        assertTrue(e.getMessage().contains("please specify which"), e.getMessage());
    }

    @Test
    void whatIsNotLaunched() throws IOException {
        // the name of a profile is not a launch target, commands look profiles up before
        undetected("main");
        assertEquals(ProfileLaunchTarget.NAME, service.parse(ProfileLaunchTarget.NAME, TargetArgs.of("main")).target().getName());
        undetected("fabric", "1.7.10");
        undetected("quilt", "1.21.1");
        undetected(root.resolve("missing.jar").toString());
        undetected(root.resolve("missing.json").toString());
        undetected(root.resolve("files").toString());
        undetected(Files.writeString(root.resolve("notes.txt"), "notes").toString());
        undetected("not-installed");
        undetected();

        // every target says why
        String message = undetected("not-installed").getMessage();
        for (LaunchTarget target : service.getTargets()) {
            assertEquals(target.isDetectable(), message.contains(" - " + target.getName() + ": "), message);
        }
    }

}

package io.github.headlesshq.headlessmc.commands.version;

import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.launcher.client.ClientTargetEnvironment;
import io.github.headlesshq.headlessmc.launcher.client.VersionRemoveService;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static io.github.headlesshq.headlessmc.launcher.client.ClientTargetEnvironment.OLD_BUILD;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code version remove} with the same version installed in two {@code .minecraft} directories.
 */
class RemoveCommandMultiDirTest {
    private static final String FABRIC = "fabric-loader-" + OLD_BUILD + "-1.21.1";

    @TempDir
    Path root;

    private final RecordingConsole console = new RecordingConsole();
    private FakeProfileService profileService;
    private ClientTargetEnvironment env;

    @BeforeEach
    void setup() {
        profileService = new FakeProfileService(root.resolve("profiles"));
        env = new ClientTargetEnvironment(root, profileService);
        env.install(env.primary, "fabric", "1.21.1", OLD_BUILD);
        env.install(env.secondary, "fabric", "1.21.1", OLD_BUILD);
    }

    private RemoveCommand command(String... versionArg) {
        RemoveCommand command = new RemoveCommand(new VersionRemoveService(
            env.matcherService, env.versionJsonService, env.platformService, profileService,
            env.fileService, env.jsonService, env.launchTargetService
        ), env.mcFilesService, env.fileService, console);
        command.setVersionArg(List.of(versionArg));
        return command;
    }

    @Test
    void aVersionInstalledInTwoDirectoriesNeedsMc() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, command("fabric", "1.21.1", OLD_BUILD)::run
        );

        assertTrue(e.getMessage().contains("is installed in multiple Minecraft directories"), e.getMessage());
        assertTrue(e.getMessage().contains("--mc <path>"), e.getMessage());
        assertTrue(Files.exists(env.json(env.primary, FABRIC)));
        assertTrue(Files.exists(env.json(env.secondary, FABRIC)));
    }

    @Test
    void mcSelectsTheDirectoryToDeleteFrom() {
        RemoveCommand command = command("fabric", "1.21.1", OLD_BUILD);
        command.setMc(env.secondary.getMcDir().toString());
        command.run();

        assertFalse(Files.exists(env.json(env.secondary, FABRIC)));
        // the other directory, and the vanilla version next to it, are untouched
        assertTrue(Files.exists(env.json(env.secondary, "1.21.1")));
        assertTrue(Files.exists(env.json(env.primary, FABRIC)));
        assertTrue(console.output().contains("Deleted versions [" + FABRIC + "]"), console.output());
    }

    @Test
    void theIdOfAVersionWorksWithMcAsWell() {
        RemoveCommand command = command(FABRIC);
        command.setMc(env.primary.getMcDir().toString());
        command.run();

        assertFalse(Files.exists(env.json(env.primary, FABRIC)));
        assertTrue(Files.exists(env.json(env.secondary, FABRIC)));
        // now it is installed once, and found without --mc
        command(FABRIC).run();
        assertFalse(Files.exists(env.json(env.secondary, FABRIC)));
    }

    @Test
    void aDirectoryThatDoesNotHaveTheVersionIsAnError() {
        RemoveCommand command = command("fabric", "1.21.1", OLD_BUILD);
        command.setMc(root.resolve("empty").toString());

        assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(Files.exists(env.json(env.primary, FABRIC)));
        assertTrue(Files.exists(env.json(env.secondary, FABRIC)));
    }

}

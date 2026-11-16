package io.github.headlesshq.headlessmc.commands;

import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.launcher.profile.FakeLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.TestLaunchTargets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DebugCommandTest {
    @TempDir
    Path root;

    @Test
    public void listsEveryMinecraftDirectoryAndLaunchTarget() throws IOException {
        AppFiles appFiles = TestFiles.appFiles(root);
        McFiles primary = McFiles.of(Files.createDirectories(root.resolve("mc")));
        // configured, but its disk is not mounted
        McFiles missing = McFiles.of(root.resolve("unmounted").resolve(".minecraft"));
        TestLaunchTargets targets = new TestLaunchTargets(new FakeProfileService(root.resolve("profiles")))
            .with(new FakeLaunchTarget("alpha"), new FakeLaunchTarget("beta"))
            .withProfiles();
        RecordingConsole console = new RecordingConsole();

        new DebugCommand(appFiles, targets.service(), McFilesService.of(primary, missing), console).run();

        List<String> lines = console.lines();
        // the primary directory first
        List<String> mcDirs = lines.stream().filter(line -> line.startsWith(" - Mc:")).toList();
        assertEquals(
            List.of(
                " - Mc:     " + primary.getMcDir().toAbsolutePath(),
                " - Mc:     " + missing.getMcDir().toAbsolutePath() + " (not available)"
            ),
            mcDirs
        );
        assertTrue(lines.contains(" - Data:   " + appFiles.getDataDir().toAbsolutePath()), lines.toString());
        // the launch targets, also those of plugins, with what they take
        assertTrue(lines.contains("Launch targets:"), lines.toString());
        assertTrue(lines.contains(" - alpha: <alpha>"), lines.toString());
        assertTrue(lines.contains(" - beta: <beta>"), lines.toString());
        assertTrue(lines.stream().anyMatch(line -> line.startsWith(" - profile: ")), lines.toString());
    }

}

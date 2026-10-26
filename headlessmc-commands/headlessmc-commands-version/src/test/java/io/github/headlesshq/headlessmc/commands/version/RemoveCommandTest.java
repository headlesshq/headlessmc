package io.github.headlesshq.headlessmc.commands.version;

import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.launcher.client.VersionRemoveService;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.client.FakeVersionMatcherService;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.github.headlesshq.headlessmc.version.service.FakeVersionJsonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RemoveCommandTest {
    private static final String FABRIC = "fabric-loader-0.16.9-1.21.1";

    @TempDir
    Path root;

    private final RecordingConsole console = new RecordingConsole();
    private final FakeVersionJsonService versionService = new FakeVersionJsonService();
    private final FakeVersionMatcherService matcherService = new FakeVersionMatcherService();
    private FakePlatformService platformService;
    private FakeProfileService profileService;
    private McFiles mcFiles;

    @BeforeEach
    void setup() throws IOException {
        mcFiles = TestFiles.mcFiles(root);
        platformService = new FakePlatformService(
            new FakeVanillaPlatform("1.21.1"),
            FakePlatform.create("fabric", new String[]{"1.21.1", "0.16.9"})
        );
        profileService = new FakeProfileService(root.resolve("profiles"));
        install(new FakeVersion("1.21.1"), "1.21.1");
        install(new FakeVersion(FABRIC).withInheritsFrom("1.21.1"), "fabric", "1.21.1", "0.16.9");
    }

    private void install(FakeVersion version, String... versionArg) throws IOException {
        versionService.add(version);
        matcherService.withMatch(version.getId(), VersionID.resolve(platformService, VersionArg.parse(versionArg)));
        Path json = versionDir(version.getId()).resolve(version.getId() + ".json");
        Files.createDirectories(json.getParent());
        Files.writeString(json, "{}");
    }

    private Path versionDir(String id) {
        return mcFiles.getVersionsDir().resolve(id);
    }

    private RemoveCommand command(String... versionArg) {
        RemoveCommand command = new RemoveCommand(new VersionRemoveService(
            matcherService,
            versionService,
            platformService,
            profileService,
            new DefaultFileService(new DefaultFileSystemProvider()),
            new DefaultJacksonJsonService(),
            mcFiles
        ), console);
        command.setVersionArg(List.of(versionArg));
        return command;
    }

    @Test
    void deletesVersion() {
        command("fabric", "1.21.1", "0.16.9").run();

        assertFalse(Files.exists(versionDir(FABRIC)));
        assertTrue(Files.exists(versionDir("1.21.1")));
        assertTrue(console.output().contains("Deleted versions [" + FABRIC + "]"));
    }

    @Test
    void dryRunOnlyListsFiles() {
        RemoveCommand command = command(FABRIC);
        command.setDryRun(true);
        command.run();

        assertTrue(Files.exists(versionDir(FABRIC)));
        assertTrue(console.output().contains(versionDir(FABRIC).toString()));
    }

    @Test
    void profilesAndDependentVersionsBlockUnlessForced() {
        profileService.save(new Profile("fabric", VersionArg.parse("fabric", "1.21.1"), root));

        IllegalArgumentException profile = assertThrows(IllegalArgumentException.class, () -> command(FABRIC).run());
        assertTrue(profile.getMessage().contains("profiles that need it: [fabric]"));
        assertTrue(Files.exists(versionDir(FABRIC)));

        IllegalArgumentException version = assertThrows(IllegalArgumentException.class, () -> command("1.21.1").run());
        assertTrue(version.getMessage().contains("versions inheriting from it: [" + FABRIC + "]"));

        RemoveCommand command = command(FABRIC);
        command.setForce(true);
        command.run();
        assertFalse(Files.exists(versionDir(FABRIC)));
    }

    @Test
    void allRequiresForce() {
        RemoveCommand command = command("1.21.1");
        command.setAll(true);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("--force"));
        assertTrue(Files.exists(versionDir("1.21.1")));
    }

    @Test
    void allDeletesDependentVersionsAndProfilesAfterConfirmation() {
        profileService.save(new Profile("fabric", VersionArg.parse("fabric", "1.21.1"), root));
        profileService.save(new Profile("vanilla", VersionArg.parse("1.21.1"), root));
        console.addInput("y");

        RemoveCommand command = command("1.21.1");
        command.setForce(true);
        command.setAll(true);
        command.run();

        assertTrue(console.output().contains("This will delete versions [1.21.1, " + FABRIC + "] and profiles [fabric, vanilla]"));
        assertFalse(Files.exists(versionDir("1.21.1")));
        assertFalse(Files.exists(versionDir(FABRIC)));
        assertTrue(profileService.getProfiles().isEmpty());
    }

    @Test
    void allDeletesNothingWithoutConfirmation() {
        profileService.save(new Profile("fabric", VersionArg.parse("fabric", "1.21.1"), root));
        console.addInput("n");

        RemoveCommand command = command("1.21.1");
        command.setForce(true);
        command.setAll(true);
        command.run();

        assertTrue(console.output().contains("Aborted"));
        assertTrue(Files.exists(versionDir("1.21.1")));
        assertTrue(Files.exists(versionDir(FABRIC)));
        assertEquals(1, profileService.getProfiles().size());
    }

}

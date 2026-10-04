package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.console.*;
import io.github.headlesshq.headlessmc.console.format.SimpleTableProvider;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchOptions;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.server.ServerService;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.os.OSService;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
import io.github.headlesshq.headlessmc.platform.client.VersionMatcher;
import io.github.headlesshq.headlessmc.version.ProcessedVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.github.headlesshq.headlessmc.version.service.VersionJsonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProfileCommandsTest {
    @TempDir
    Path root;

    private final RecordingConsole console = new RecordingConsole();
    private final ArgSplitter splitter = line -> line.split(" ");
    private final TableProvider tables = new SimpleTableProvider();

    private final List<VersionID> installedClients = new ArrayList<>();
    private final Map<String, Version> versionJsons = new HashMap<>();
    private final ClientSupport clientSupport = new ClientSupport(
        Mockito.mock(VersionMatcher.class),
        (id, mcDir, args) -> {
            installedClients.add(id);
            return versionJsons.getOrDefault(id.getVersion().getName(), Mockito.mock(Version.class));
        }
    );

    private final FakePlatformService platformService = new FakePlatformService(
        new FakeVanillaPlatform("1.21.1", "1.20.4").withClientSupport(clientSupport),
        FakePlatform.create("fabric", new String[]{"1.21.1", "0.16.9"}).withClientSupport(clientSupport)
    );

    private final VersionJsonService versionJsonService = Mockito.mock(VersionJsonService.class);
    private final OSService osService = Mockito.mock(OSService.class);

    private FakeProfileService profileService;

    @BeforeEach
    void setup() {
        profileService = new FakeProfileService(root);
        Mockito.when(versionJsonService.process(Mockito.any()))
            .thenAnswer(invocation -> new ProcessedVersion(List.of(invocation.<Version>getArgument(0))));
        Mockito.when(osService.getCPU()).thenReturn(CPU.X64);
        Mockito.when(osService.getOS()).thenReturn(new OS("linux", OS.Type.LINUX, "6.0"));
    }

    private AddCommand add() {
        return new AddCommand(
            platformService, profileService, Mockito.mock(ServerService.class),
            new DefaultFileService(new DefaultFileSystemProvider()), TestFiles.appFiles(root), TestFiles.mcFiles(root),
            console, versionJsonService, osService
        );
    }

    @Test
    void addWithoutVersionThrows() {
        assertThrows(IllegalArgumentException.class, add()::run);
    }

    @Test
    void addCreatesProfileWithDefaultName() {
        AddCommand command = add();
        command.setVersionArg(List.of("fabric", "1.21.1"));
        command.run();

        Profile profile = profileService.getProfile("fabric-1.21.1").orElseThrow();
        assertEquals("fabric", profile.version().platform());
        assertEquals("1.21.1", profile.version().version());
        assertTrue(profile.hasDefaultClientJvmArgs());
        assertTrue(console.output().contains("Added profile fabric-1.21.1"));
        assertEquals(1, installedClients.size());
    }

    @Test
    void addStoresTheDefaultJvmArgsOfTheVersionInTheProfile() {
        Version version = Mockito.mock(Version.class);
        Mockito.when(version.getArguments()).thenReturn(Map.of(
            Version.DEFAULT_JVM_ARGUMENTS, List.of(jvmArgument("-Xmx2G", "-XX:+UseG1GC"))
        ));
        versionJsons.put("1.21.1", version);

        AddCommand command = add();
        command.setVersionArg(List.of("1.21.1"));
        command.run();

        Profile profile = profileService.getProfile("1.21.1").orElseThrow();
        assertEquals(List.of("-Xmx2G", "-XX:+UseG1GC"), profile.vmArgs());
        assertTrue(profile.hasDefaultClientJvmArgs());
    }

    private static Version.Argument jvmArgument(String... values) {
        return new Version.Argument() {
            @Override
            public @org.jspecify.annotations.Nullable List<Version.Rule> getRules() {
                return null;
            }

            @Override
            public List<String> value() {
                return List.of(values);
            }
        };
    }

    @Test
    void addUsesCustomNameAndDir(@TempDir Path custom) {
        AddCommand command = add();
        command.setVersionArg(List.of("1.21.1"));
        command.setName("main");
        command.setCustomDir(custom.toString());
        command.run();

        Profile profile = profileService.getProfile("main").orElseThrow();
        assertEquals(custom, profile.path());
    }

    @Test
    void addExistingProfileRequiresForce() {
        profileService.createDefault("main", VersionArg.parse("1.21.1"));

        AddCommand command = add();
        command.setVersionArg(List.of("1.20.4"));
        command.setName("main");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("already exists"));

        command.setForce(true);
        command.run();
        assertEquals("1.20.4", profileService.getProfile("main").orElseThrow().version().version());
    }

    @Test
    void listShowsProfiles() {
        profileService.createDefault("main", VersionArg.parse("fabric", "1.21.1"));

        new ListCommand(profileService, tables, console).run();

        String output = console.output();
        assertTrue(output.contains("main"));
        assertTrue(output.contains("fabric/1.21.1"));
    }

    @Test
    void removeDeletesProfile() {
        profileService.createDefault("main", VersionArg.parse("1.21.1"));

        RemoveCommand command = new RemoveCommand(profileService, console);
        command.setName("main");
        command.run();

        assertTrue(profileService.getProfile("main").isEmpty());
        assertTrue(console.output().contains("Removed profile main"));
    }

    @Test
    void removeUnknownProfileThrows() {
        RemoveCommand command = new RemoveCommand(profileService, console);
        assertThrows(IllegalArgumentException.class, command::run);

        command.setName("nope");
        assertThrows(IllegalArgumentException.class, command::run);
    }

    private EditCommand edit(RecordingConsole console) {
        return new EditCommand(new EditFieldHelper(platformService, splitter), profileService, tables, splitter, console);
    }

    @Test
    void editWithoutFieldListsFields() {
        profileService.createDefault("main", VersionArg.parse("1.21.1"));

        EditCommand command = edit(console);
        command.setName("main");
        command.run();

        String output = console.output();
        assertTrue(output.contains("name"));
        assertTrue(output.contains("version"));
        assertTrue(output.contains("path"));
        assertTrue(output.contains("vm-args"));
        assertTrue(output.contains("game-args"));
        assertTrue(output.contains("resolution"));
    }

    @Test
    void serverProfileHasNoClientFields() {
        profileService.createDefault("srv", VersionArg.parse("server", "1.21.1"));

        EditCommand command = edit(console);
        command.setName("srv");
        command.run();

        String output = console.output();
        assertTrue(output.contains("version"));
        assertTrue(output.contains("vm-args"));
        assertFalse(output.contains("resolution"));
    }

    @Test
    void editUnknownFieldThrows() {
        profileService.createDefault("main", VersionArg.parse("1.21.1"));

        EditCommand command = edit(console);
        command.setName("main");
        command.setField("nope");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("available"));
    }

    @Test
    void editUnknownProfileThrows() {
        EditCommand command = edit(console);
        command.setName("nope");
        assertThrows(IllegalArgumentException.class, command::run);

        command.setName(null);
        assertThrows(IllegalArgumentException.class, command::run);
    }

    @Test
    void renameSavesNewAndRemovesOldProfile() {
        profileService.createDefault("main", VersionArg.parse("1.21.1"));

        EditCommand command = edit(console);
        command.setName("main");
        command.setField("name");
        command.setValue("renamed");
        command.run();

        assertTrue(profileService.getProfile("main").isEmpty());
        assertTrue(profileService.getProfile("renamed").isPresent());
    }

    @Test
    void editVersionUpdatesCurrentVersion() {
        profileService.createDefault("main", VersionArg.parse("1.21.1"));

        EditCommand command = edit(console);
        command.setName("main");
        command.setField("version");
        command.setValue("fabric 1.20.4");
        command.run();

        Profile profile = profileService.getProfile("main").orElseThrow();
        assertEquals("fabric/1.20.4", profile.version().toString());
        assertEquals(profile.version(), profile.currentVersion());
    }

    @Test
    void editVersionRejectsUnresolvableVersions() {
        profileService.createDefault("main", VersionArg.parse("1.21.1"));

        EditCommand command = edit(console);
        command.setName("main");
        command.setField("version");
        command.setValue("1.19");

        assertThrows(RuntimeException.class, command::run);
        assertEquals("1.21.1", profileService.getProfile("main").orElseThrow().version().version());
    }

    @Test
    void editVmArgsSplitsSystemProperties() {
        profileService.createDefault("main", VersionArg.parse("1.21.1"));

        EditCommand command = edit(console);
        command.setName("main");
        command.setField("vm-args");
        command.setValue("-Xmx4G -Dkey=value -Dflag");
        command.run();

        Profile profile = profileService.getProfile("main").orElseThrow();
        assertEquals(List.of("-Xmx4G"), profile.vmArgs());
        assertEquals("value", profile.systemProperties().get("key"));
        assertTrue(profile.systemProperties().containsKey("flag"));
    }

    @Test
    void editGameArgsReplacesTheGameArgs() {
        profileService.createDefault("main", VersionArg.parse("1.21.1"));

        EditCommand command = edit(console);
        command.setName("main");
        command.setField("game-args");
        command.setValue("--demo --width 100");
        command.run();

        assertEquals(List.of("--demo", "--width", "100"), profileService.getProfile("main").orElseThrow().gameArgs());
    }

    @Test
    void editResolutionParsesValue() {
        profileService.createDefault("main", VersionArg.parse("1.21.1"));

        EditCommand command = edit(console);
        command.setName("main");
        command.setField("resolution");
        command.setValue("1024x768");
        command.run();

        Profile profile = profileService.getProfile("main").orElseThrow();
        assertEquals(Optional.of(new LaunchOptions.Resolution(1024, 768)), profile.options().resolution());
    }

    @Test
    void editWithoutValueRequiresConsoleExtensions() {
        profileService.createDefault("main", VersionArg.parse("1.21.1"));

        EditCommand command = edit(console);
        command.setName("main");
        command.setField("path");
        assertThrows(ConsoleException.class, command::run);
    }

    @Test
    void editWithConsoleExtensionsUsesEditedValue(@TempDir Path newPath) {
        profileService.createDefault("main", VersionArg.parse("1.21.1"));
        RecordingConsole editingConsole = new RecordingConsole() {
            @Override
            public Optional<ConsoleExtensions> extensions() {
                return Optional.of(new ConsoleExtensions() {
                    @Override
                    public String edit(String initialString) {
                        return newPath.toString();
                    }

                    @Override
                    public String read(String prompt, Completions completions) {
                        return "";
                    }

                    @Override
                    public int getWidth() {
                        return 80;
                    }
                });
            }
        };

        EditCommand command = edit(editingConsole);
        command.setName("main");
        command.setField("path");
        command.run();

        assertEquals(newPath, profileService.getProfile("main").orElseThrow().path());
    }

}

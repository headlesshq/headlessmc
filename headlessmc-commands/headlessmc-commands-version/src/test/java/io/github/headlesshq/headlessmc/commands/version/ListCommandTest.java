package io.github.headlesshq.headlessmc.commands.version;

import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.console.format.SimpleTableProvider;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.client.FakeVersionMatcherService;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.github.headlesshq.headlessmc.version.service.FakeVersionJsonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ListCommandTest {
    private final RecordingConsole console = new RecordingConsole();
    private final TableProvider tables = new SimpleTableProvider();

    private FakePlatformService platformService;
    private FakeVersionJsonService versionService;
    private FakeVersionMatcherService matcherService;

    @BeforeEach
    void setup() {
        FakeVanillaPlatform vanilla = new FakeVanillaPlatform("1.21.1", "1.20.4");
        FakePlatform fabric = FakePlatform.create(
            "fabric",
            new String[]{"1.21.1", "0.16.9", "0.16.5"},
            new String[]{"1.20.4", "0.15.0"}
        );
        platformService = new FakePlatformService(vanilla, fabric);
        versionService = new FakeVersionJsonService(
            new FakeVersion("1.21.1").withType("release"),
            new FakeVersion("fabric-loader-0.16.9-1.21.1").withInheritsFrom("1.21.1").withType("release"),
            new FakeVersion("24w14a").withType("snapshot")
        );
        matcherService = new FakeVersionMatcherService()
            .withMatch("fabric-loader-0.16.9-1.21.1", VersionID.resolve(
                platformService,
                new VersionArg(Optional.empty(), "fabric", "1.21.1", Optional.of("0.16.9"))
            ));
    }

    private ListCommand command() {
        return new ListCommand(matcherService, versionService, tables, platformService, console);
    }

    @Test
    void listsAllInstalledVersions() {
        command().run();

        String output = console.output();
        assertTrue(output.contains("1.21.1"));
        assertTrue(output.contains("fabric-loader-0.16.9-1.21.1"));
        assertTrue(output.contains("24w14a"));
        assertTrue(output.contains("snapshot"));
    }

    @Test
    void filtersByType() {
        ListCommand command = command();
        command.setType("snapshot");
        command.run();

        String output = console.output();
        assertTrue(output.contains("24w14a"));
        assertFalse(output.contains("fabric-loader"));
    }

    @Test
    void unknownTypeThrows() {
        ListCommand command = command();
        command.setType("beta");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("beta"));
        assertTrue(e.getMessage().contains("snapshot"));
    }

    @Test
    void filtersByMatchedPlatformParameter() {
        ListCommand command = command();
        command.setParameters(List.of("fabric"));
        command.run();

        String output = console.output();
        assertTrue(output.contains("fabric-loader-0.16.9-1.21.1"));
        assertFalse(output.contains("24w14a"));
    }

    @Test
    void filtersRequireAllParametersToMatch() {
        ListCommand command = command();
        command.setParameters(List.of("fabric", "24w14a"));
        command.run();

        String output = console.output();
        assertFalse(output.contains("fabric-loader"));
        assertFalse(output.contains("24w14a"));
    }

    @Test
    void listsRemoteBuildsForPlatformAndVersion() {
        ListCommand command = command();
        command.setRemote(true);
        command.setParameters(List.of("fabric", "1.21.1"));
        command.run();

        String output = console.output();
        assertTrue(output.contains("0.16.9"));
        assertTrue(output.contains("0.16.5"));
        assertFalse(output.contains("0.15.0"), "1.20.4 builds should not be listed");
    }

    @Test
    void remoteListingWithoutParametersListsAllNonVanillaPlatforms() {
        ListCommand command = command();
        command.setRemote(true);
        command.run();

        String output = console.output();
        assertTrue(output.contains("0.16.9"));
        assertTrue(output.contains("0.15.0"));
    }

    @Test
    void remoteListingFiltersByOtherParameter() {
        ListCommand command = command();
        command.setRemote(true);
        command.setParameters(List.of("fabric", "0.16.5"));
        command.run();

        String output = console.output();
        assertTrue(output.contains("0.16.5"));
        assertFalse(output.contains("0.16.9"));
    }

}

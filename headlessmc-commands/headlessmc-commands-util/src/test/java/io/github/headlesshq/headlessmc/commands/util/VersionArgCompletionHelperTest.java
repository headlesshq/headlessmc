package io.github.headlesshq.headlessmc.commands.util;

import io.github.headlesshq.headlessmc.console.Completions;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
import io.github.headlesshq.headlessmc.platform.server.ServerInstaller;
import io.github.headlesshq.headlessmc.platform.server.ServerSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VersionArgCompletionHelperTest {
    private VersionArgCompletionHelper helper;

    @BeforeEach
    void setup() {
        FakeVanillaPlatform vanilla = new FakeVanillaPlatform("1.21.1", "1.20.4");
        vanilla.withClientSupport(new ClientSupport(null, null));
        vanilla.withServerSupport(serverSupport());

        FakePlatform fabric = FakePlatform.create(
            "fabric", new String[]{"1.21.1", "0.16.9", "0.16.5"}, new String[]{"1.20.4", "0.15.0"}
        );
        fabric.withClientSupport(new ClientSupport(null, null));

        FakePlatform paper = FakePlatform.create("paper", new String[]{"1.21.1", "125", "124"});
        paper.withServerSupport(serverSupport());

        helper = new VersionArgCompletionHelper(new FakePlatformService(vanilla, fabric, paper));
    }

    private ServerSupport serverSupport() {
        return new ServerSupport(
            dir -> dir.resolve("server.jar"),
            (id, dir, args) -> new ServerInstaller.Installation(21)
        );
    }

    private List<String> names(List<Completions.Candidate> candidates) {
        return candidates.stream().map(Completions.Candidate::getName).toList();
    }

    private List<String> complete(String... args) {
        return names(helper.complete(args.length == 0 ? List.of() : List.of(args)));
    }

    @Test
    void nullAndEmptyArgsListPlatformsAndSides() {
        List<String> expected = List.of("vanilla", "fabric", "paper", "client", "server");
        assertEquals(expected, names(helper.complete(null)));
        assertEquals(expected, complete());
    }

    @Test
    void firstArgAlsoCompletesVanillaVersions() {
        List<String> result = complete("1");
        assertTrue(result.contains("fabric"));
        assertTrue(result.contains("1.21.1"));
        assertTrue(result.contains("1.20.4"));
    }

    @Test
    void firstArgWithLetterPrefixDoesNotCompleteVanillaVersions() {
        List<String> result = complete("fa");
        assertTrue(result.contains("fabric"));
        assertFalse(result.contains("1.21.1"));
    }

    @Test
    void firstArgWithReleaseLikePrefixCompletesVanillaVersions() {
        assertTrue(complete("b").contains("1.21.1"));
        assertTrue(complete("r").contains("1.21.1"));
    }

    @Test
    void sideCompletesPlatformsSupportingThatSide() {
        List<String> client = complete("client", "");
        assertTrue(client.contains("fabric"));
        assertFalse(client.contains("paper"));

        List<String> server = complete("server", "");
        assertTrue(server.contains("paper"));
        assertFalse(server.contains("fabric"));
    }

    @Test
    void platformCompletesVanillaVersions() {
        assertEquals(List.of("1.21.1", "1.20.4"), complete("fabric", "1."));
    }

    @Test
    void unknownFirstArgCompletesNothing() {
        assertEquals(List.of(), complete("1.21.1", "any"));
    }

    @Test
    void sideAndPlatformCompleteVanillaVersions() {
        assertEquals(List.of("1.21.1", "1.20.4"), complete("client", "fabric", "1."));
    }

    @Test
    void sideAndPlatformWithoutSupportCompletesNothing() {
        assertEquals(List.of(), complete("server", "fabric", "1."));
        assertEquals(List.of(), complete("client", "unknown", "1."));
    }

    @Test
    void platformAndVersionCompleteBuilds() {
        assertEquals(List.of("0.16.9", "0.16.5"), complete("fabric", "1.21.1", ""));
    }

    @Test
    void unknownPlatformOrVersionCompletesNoBuilds() {
        assertEquals(List.of(), complete("unknown", "1.21.1", ""));
        assertEquals(List.of(), complete("fabric", "1.7.10", ""));
    }

    @Test
    void sidePlatformAndVersionCompleteBuilds() {
        assertEquals(List.of("0.16.9", "0.16.5"), complete("client", "fabric", "1.21.1", ""));
    }

    @Test
    void fourArgsWithoutSideCompletesNothing() {
        assertEquals(List.of(), complete("fabric", "1.21.1", "0.16.9", ""));
    }

    @Test
    void fourArgsWithUnsupportedSideCompletesNothing() {
        assertEquals(List.of(), complete("server", "fabric", "1.21.1", ""));
    }

    @Test
    void tooManyArgsCompleteNothing() {
        assertEquals(List.of(), complete("client", "fabric", "1.21.1", "0.16.9", ""));
    }

}

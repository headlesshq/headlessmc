package io.github.headlesshq.headlessmc.commands.util;

import io.github.headlesshq.headlessmc.console.Completions;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompletionsTest {
    private record Command(List<String> getVersionArg) implements VersionArgCommand {}

    @TempDir
    Path root;

    private VersionArgCompletionHelper helper;
    private FakeProfileService profileService;

    @BeforeEach
    void setup() {
        FakePlatform fabric = FakePlatform.create("fabric", new String[]{"1.21.1", "0.16.9"});
        helper = new VersionArgCompletionHelper(
            new FakePlatformService(new FakeVanillaPlatform("1.21.1"), fabric)
        );
        profileService = new FakeProfileService(root);
    }

    private List<String> names(Iterable<Completions.Candidate> candidates) {
        List<String> result = new ArrayList<>();
        candidates.forEach(candidate -> result.add(candidate.getName()));
        return result;
    }

    @Test
    void versionCompletionsDelegateToHelper() {
        VersionCompletions completions = new VersionCompletions(helper);
        List<String> result = names(completions.candidates(new Command(List.of("fabric", "1.")), null));

        assertEquals(List.of("1.21.1"), result);
        assertFalse(completions.iterator().hasNext());
    }

    @Test
    void versionCompletionsWithForeignSpecAreEmpty() {
        VersionCompletions completions = new VersionCompletions(helper);
        assertEquals(List.of(), names(completions.candidates("not a command", null)));
        assertEquals(List.of(), names(completions.candidates(null, null)));
    }

    @Test
    void profileVersionCompletionsListProfilesAndVersions() {
        profileService.createDefault("main", VersionArg.parse("fabric", "1.21.1"));

        ProfileVersionCompletions completions = new ProfileVersionCompletions(helper, profileService);
        List<String> result = names(completions.candidates(new Command(List.of("fabric", "1.")), null));

        assertEquals(List.of("main", "1.21.1"), result);
        assertFalse(completions.iterator().hasNext());
    }

    @Test
    void profileVersionCompletionsWithForeignSpecOnlyListProfiles() {
        profileService.createDefault("main", VersionArg.parse("fabric", "1.21.1"));

        ProfileVersionCompletions completions = new ProfileVersionCompletions(helper, profileService);
        assertEquals(List.of("main"), names(completions.candidates("not a command", null)));
    }

}

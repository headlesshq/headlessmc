package io.github.headlesshq.headlessmc.commands.util;

import io.github.headlesshq.headlessmc.console.Completions;
import io.github.headlesshq.headlessmc.launcher.profile.FakeLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.TestLaunchTargets;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    private record TargetCommand(
        @Nullable String getTarget,
        List<String> getTargetArgs,
        Set<Side> getSides,
        boolean acceptsProfileNames
    ) implements LaunchTargetCommand {
        private TargetCommand(@Nullable String target, String... args) {
            this(target, List.of(args), Side.BOTH, true);
        }
    }

    private LaunchTargetCompletions launchTargetCompletions(LaunchTarget... targets) {
        return new LaunchTargetCompletions(new TestLaunchTargets(profileService).with(targets).service(), profileService);
    }

    @Test
    void launchTargetCompletionsUseTheNamedTarget() {
        profileService.createDefault("main", VersionArg.parse("fabric", "1.21.1"));
        LaunchTargetCompletions completions = launchTargetCompletions(
            new FakeLaunchTarget("alpha").withCandidates("a1", "a2"),
            new FakeLaunchTarget("beta").withCandidates("b1")
        );

        // only what the named target completes, the argument is not the name of a profile then
        assertEquals(List.of("a1", "a2"), names(completions.candidates(new TargetCommand("alpha", ""), null)));
        assertEquals(List.of(), names(completions.candidates(new TargetCommand("gamma", ""), null)));
        assertFalse(completions.iterator().hasNext());
    }

    @Test
    void launchTargetCompletionsWithoutTargetOfferProfilesAndAllTargets() {
        profileService.createDefault("main", VersionArg.parse("fabric", "1.21.1"));
        LaunchTargetCompletions completions = launchTargetCompletions(
            new FakeLaunchTarget("alpha").withCandidates("a1", "shared", "main"),
            new FakeLaunchTarget("beta").withCandidates("shared", "b1"),
            new FakeLaunchTarget("hidden").detectable(false).withCandidates("hidden-candidate")
        );

        // profiles first, then what the detectable targets complete, without duplicates
        assertEquals(
            List.of("main", "a1", "shared", "b1"),
            names(completions.candidates(new TargetCommand(null, ""), null))
        );
    }

    @Test
    void profileNamesAreOnlyOfferedForTheFirstArgument() {
        profileService.createDefault("main", VersionArg.parse("fabric", "1.21.1"));
        LaunchTargetCompletions completions = launchTargetCompletions(new FakeLaunchTarget("alpha").withCandidates("a1"));

        assertEquals(List.of("a1"), names(completions.candidates(new TargetCommand(null, "fabric", ""), null)));
        assertEquals(List.of("main", "a1"), names(completions.candidates(new TargetCommand(null), null)));
    }

    @Test
    void profileNamesAreOnlyOfferedByCommandsThatAcceptThem() {
        profileService.createDefault("main", VersionArg.parse("fabric", "1.21.1"));
        LaunchTargetCompletions completions = launchTargetCompletions(new FakeLaunchTarget("alpha").withCandidates("a1"));

        // e.g. "profile add", whose arguments are never the name of a profile
        TargetCommand add = new TargetCommand(null, List.of(""), Side.BOTH, false);
        assertEquals(List.of("a1"), names(completions.candidates(add, null)));
    }

    @Test
    void profilesOfSidesTheCommandDoesNotLaunchAreNotOffered() {
        profileService.createDefault("client", VersionArg.parse("fabric", "1.21.1"));
        profileService.createDefault("srv", VersionArg.parse("server", "fabric", "1.21.1"));
        LaunchTargetCompletions completions = launchTargetCompletions();

        TargetCommand serverCommand = new TargetCommand(null, List.of(""), Set.of(Side.SERVER), true);
        assertEquals(List.of("srv"), names(completions.candidates(serverCommand, null)));
        assertEquals(List.of("client", "srv"), names(completions.candidates(new TargetCommand(null, ""), null)));
    }

    @Test
    void launchTargetCompletionsWithForeignSpecAreEmpty() {
        LaunchTargetCompletions completions = launchTargetCompletions(new FakeLaunchTarget("alpha").withCandidates("a1"));

        assertEquals(List.of(), names(completions.candidates("not a command", null)));
        assertEquals(List.of(), names(completions.candidates(null, null)));
    }

    @Test
    void launchTargetCompletionsHandleMissingArguments() {
        LaunchTargetCompletions completions = launchTargetCompletions(new FakeLaunchTarget("alpha").withCandidates("a1"));

        assertEquals(List.of("a1"), names(completions.candidates(new TargetCommand(null, null, Side.BOTH, true), null)));
    }

    @Test
    void targetNameCompletionsListTheLaunchTargets() {
        LaunchTargetNameCompletions completions = new LaunchTargetNameCompletions(
            new TestLaunchTargets(profileService).with(new FakeLaunchTarget("beta"), new FakeLaunchTarget("alpha")).service()
        );

        List<Completions.Candidate> candidates = new ArrayList<>();
        completions.candidates(null, null).forEach(candidates::add);
        assertEquals(List.of("alpha", "beta"), names(candidates));
        // the usage of the target is shown as description
        assertEquals("<alpha>", candidates.getFirst().getDescription());
        List<String> iterated = new ArrayList<>();
        completions.iterator().forEachRemaining(iterated::add);
        assertEquals(List.of("alpha", "beta"), iterated);
    }

    @Test
    void profileVersionCompletionsWithForeignSpecOnlyListProfiles() {
        profileService.createDefault("main", VersionArg.parse("fabric", "1.21.1"));

        ProfileVersionCompletions completions = new ProfileVersionCompletions(helper, profileService);
        assertEquals(List.of("main"), names(completions.candidates("not a command", null)));
    }

}

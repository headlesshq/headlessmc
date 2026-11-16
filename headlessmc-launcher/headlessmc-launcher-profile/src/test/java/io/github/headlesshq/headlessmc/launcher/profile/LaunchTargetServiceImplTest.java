package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.arg.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LaunchTargetServiceImplTest {
    @TempDir
    Path root;

    private FakeProfileService profileService;
    private TestLaunchTargets targets;
    private LaunchTargetService service;
    private FakeLaunchTarget alpha;
    private FakeLaunchTarget beta;

    @BeforeEach
    void setup() {
        profileService = new FakeProfileService(root);
        targets = new TestLaunchTargets(profileService);
        service = targets.service();
        // alpha only takes arguments starting with "a", beta those starting with "b"
        alpha = new FakeLaunchTarget("alpha").parsingOnly("a");
        beta = new FakeLaunchTarget("beta").parsingOnly("b");
        targets.with(beta, alpha);
    }

    private Profile profile(String name, FakeLaunchTarget target, String value) {
        return new Profile(name, Optional.empty(), target.handle(value), root.resolve(name));
    }

    // lookup

    @Test
    void targetsAreFoundByNameIgnoringCase() {
        assertSame(alpha, service.getTarget("alpha").orElseThrow());
        assertSame(alpha, service.getTarget("ALPHA").orElseThrow());
        assertSame(beta, service.require("Beta"));
        assertEquals(Optional.empty(), service.getTarget("gamma"));
    }

    @Test
    void targetsAreSortedByName() {
        assertEquals(List.of(alpha, beta), service.getTargets());
    }

    @Test
    void anUnknownTargetNameListsTheAvailableTargets() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.require("gamma"));

        assertTrue(e.getMessage().contains("'gamma'"), e.getMessage());
        assertTrue(e.getMessage().contains("alpha, beta"), e.getMessage());
    }

    @Test
    void twoTargetsWithTheSameNameAreAnError() {
        targets.with(new FakeLaunchTarget("alpha"));

        assertThrows(IllegalStateException.class, () -> service.getTarget("alpha"));
    }

    @Test
    void targetsAddedLaterAreSeen() {
        FakeLaunchTarget gamma = new FakeLaunchTarget("gamma");
        targets.with(gamma);

        assertSame(gamma, service.require("gamma"));
    }

    @Test
    void theTargetOfAProfileThatIsNotAvailableIsReported() {
        Profile profile = new Profile("p", Optional.empty(), new LaunchTargetHandle("from-a-plugin", "x"), root);

        LaunchException e = assertThrows(LaunchException.class, () -> service.require(profile));

        assertTrue(e.getMessage().contains("'from-a-plugin'"), e.getMessage());
        assertTrue(e.getMessage().contains("profile p"), e.getMessage());
        assertTrue(e.getMessage().contains("plugin"), e.getMessage());
    }

    // parsing with a named target

    @Test
    void aNamedTargetParsesTheArguments() {
        LaunchTargetService.TargetMatch match = service.parse("alpha", TargetArgs.of("a1"));

        assertSame(alpha, match.target());
        assertEquals("a1", FakeLaunchTarget.value(match.parsed().handle()));
        // only the named target has been asked
        assertEquals(1, alpha.parsed().size());
        assertEquals(0, beta.parsed().size());
    }

    @Test
    void aNamedTargetThatCannotParseReportsWhyAndItsUsage() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> service.parse("alpha", TargetArgs.of("b1"))
        );

        assertTrue(e.getMessage().contains("alpha"), e.getMessage());
        assertTrue(e.getMessage().contains("'b1'"), e.getMessage());
        assertTrue(e.getMessage().contains("does not start with a"), e.getMessage());
        assertTrue(e.getMessage().contains("<alpha>"), e.getMessage());
    }

    @Test
    void aNamedTargetThatDoesNotExistIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> service.parse("gamma", TargetArgs.of("a1")));
    }

    @Test
    void aNamedTargetForASideTheCommandDoesNotLaunchIsRejected() {
        alpha.withSide(Side.CLIENT);

        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> service.parse("alpha", new TargetArgs(List.of("a1"), Set.of(Side.SERVER)))
        );
        assertTrue(e.getMessage().contains("a client"), e.getMessage());
    }

    // detection

    @Test
    void theOneTargetThatCanParseTheArgumentsIsDetected() {
        LaunchTargetService.TargetMatch a = service.parse(null, TargetArgs.of("a1"));
        LaunchTargetService.TargetMatch b = service.parse(null, TargetArgs.of("b1"));

        assertSame(alpha, a.target());
        assertSame(beta, b.target());
    }

    @Test
    void detectionParsesEveryTargetExactlyOnce() {
        service.parse(null, TargetArgs.of("a1"));

        // the result of parsing is used, the arguments are not parsed a second time for the match
        assertEquals(1, alpha.parsed().size());
        assertEquals(1, beta.parsed().size());
    }

    @Test
    void argumentsThatTwoTargetsCanParseAreAmbiguous() {
        targets.with(new FakeLaunchTarget("gamma").parsingOnly("a"));

        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> service.parse(null, TargetArgs.of("a1"))
        );

        assertTrue(e.getMessage().contains("ambiguous"), e.getMessage());
        assertTrue(e.getMessage().contains("alpha, gamma"), e.getMessage());
        assertTrue(e.getMessage().contains("--target"), e.getMessage());
        // naming the target solves it
        assertSame(alpha, service.parse("alpha", TargetArgs.of("a1")).target());
    }

    @Test
    void aFallbackIsOnlyUsedIfNoOtherTargetCanParseTheArguments() {
        // gamma can launch a1 as well, but says that it is probably not what is meant
        FakeLaunchTarget gamma = new FakeLaunchTarget("gamma").failingIf(args -> null).fallback(true);
        targets.with(gamma);

        LaunchTargetService.TargetMatch contested = service.parse(null, TargetArgs.of("a1"));
        LaunchTargetService.TargetMatch alone = service.parse(null, TargetArgs.of("c1"));

        assertSame(alpha, contested.target());
        assertFalse(contested.parsed().fallback());
        assertSame(gamma, alone.target());
        assertTrue(alone.parsed().fallback());
        // naming the target always uses it
        assertSame(gamma, service.parse("gamma", TargetArgs.of("a1")).target());
    }

    @Test
    void twoFallbacksAreAmbiguous() {
        targets.with(
            new FakeLaunchTarget("gamma").failingIf(args -> null).fallback(true),
            new FakeLaunchTarget("delta").failingIf(args -> null).fallback(true)
        );

        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> service.parse(null, TargetArgs.of("c1"))
        );

        assertTrue(e.getMessage().contains("delta, gamma"), e.getMessage());
        // with a target that is sure about the arguments there is nothing to decide
        assertSame(alpha, service.parse(null, TargetArgs.of("a1")).target());
    }

    @Test
    void argumentsNoTargetCanParseListTheReasonOfEveryTarget() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> service.parse(null, TargetArgs.of("c1", "x"))
        );

        String message = e.getMessage();
        assertTrue(message.contains("No launch target can launch 'c1 x'"), message);
        assertTrue(message.contains("alpha: does not start with a (<alpha>)"), message);
        assertTrue(message.contains("beta: does not start with b (<beta>)"), message);
    }

    @Test
    void targetsThatAreNotDetectableAreOnlyUsedWhenNamed() {
        FakeLaunchTarget hidden = new FakeLaunchTarget("hidden").detectable(false);
        targets.with(hidden);

        // it could parse "a1", but it does not take part in detection, so this is not ambiguous
        assertSame(alpha, service.parse(null, TargetArgs.of("a1")).target());
        assertEquals(0, hidden.parsed().size());
        assertSame(hidden, service.parse("hidden", TargetArgs.of("anything")).target());
        // and what only it could parse is not detected
        assertThrows(IllegalArgumentException.class, () -> service.parse(null, TargetArgs.of("z")));
    }

    @Test
    void theSidesOfACommandFilterDetection() {
        alpha.withSide(Side.CLIENT);
        FakeLaunchTarget server = new FakeLaunchTarget("gamma").parsingOnly("a").withSide(Side.SERVER);
        targets.with(server);

        // both can parse it, but each command only launches one of the sides
        assertSame(alpha, service.parse(null, new TargetArgs(List.of("a1"), Set.of(Side.CLIENT))).target());
        assertSame(server, service.parse(null, new TargetArgs(List.of("a1"), Set.of(Side.SERVER))).target());
        assertThrows(IllegalArgumentException.class, () -> service.parse(null, TargetArgs.of("a1")));
    }

    @Test
    void somethingWithoutASideIsOnlyDetectedByCommandsLaunchingEverything() {
        // alpha has no side
        assertSame(alpha, service.parse(null, TargetArgs.of("a1")).target());
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> service.parse(null, new TargetArgs(List.of("a1"), Set.of(Side.SERVER)))
        );
        assertTrue(e.getMessage().contains("neither a client nor a server"), e.getMessage());
    }

    // completion

    @Test
    void completionUsesTheNamedTarget() {
        alpha.withCandidates("a1", "a2");
        beta.withCandidates("b1");

        assertEquals(List.of("a1", "a2"), values(service.complete("alpha", TargetArgs.of(""))));
        assertEquals(List.of(), values(service.complete("gamma", TargetArgs.of(""))));
    }

    @Test
    void completionWithoutATargetIsTheUnionOfAllDetectableTargets() {
        alpha.withCandidates("a1", "shared");
        beta.withCandidates("shared", "b1");
        targets.with(new FakeLaunchTarget("hidden").detectable(false).withCandidates("hidden-candidate"));

        assertEquals(List.of("a1", "shared", "b1"), values(service.complete(null, TargetArgs.of(""))));
    }

    @Test
    void aTargetFailingToCompleteDoesNotBreakCompletion() {
        beta.withCandidates("b1");
        targets.with(new FakeLaunchTarget("broken") {
            @Override
            public List<ArgCandidate> complete(TargetArgs args) {
                throw new IllegalStateException("broken");
            }
        });

        assertEquals(List.of("b1"), values(service.complete(null, TargetArgs.of(""))));
    }

    private static List<String> values(List<ArgCandidate> candidates) {
        return candidates.stream().map(ArgCandidate::value).toList();
    }

    // describing

    @Test
    void aProfileWithoutItsTargetIsDescribedAsUnknown() {
        Profile profile = new Profile("p", Optional.of(Side.SERVER), new LaunchTargetHandle("from-a-plugin", "x"), root);

        assertEquals(TargetInfo.EMPTY, service.describe(profile));
        assertEquals("from-a-plugin", service.getLabel(profile));
        // its side does not depend on its target
        assertTrue(profile.isServer());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.requireVersion(profile));
        assertTrue(e.getMessage().contains("not available"), e.getMessage());
    }

    @Test
    void aProfileThatLaunchesNoVersionHasNone() {
        Profile profile = profile("p", alpha, "a1");

        assertEquals("alpha:a1", service.getLabel(profile));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.requireVersion(profile));
        assertTrue(e.getMessage().contains("does not launch a known Minecraft version"), e.getMessage());
    }

    // profiles

    @Test
    void createUsesTheDefaultNameOfTheTarget() {
        alpha.withDefaultName("default-name");
        LaunchTargetService.TargetMatch match = service.parse(null, TargetArgs.of("a1"));

        assertEquals("default-name", service.create(null, match, new TypedMapImpl()).name());
        assertEquals("given", service.create("given", match, new TypedMapImpl()).name());
    }

    @Test
    void createWithoutANameFailsForTargetsWithoutADefaultName() {
        LaunchTargetService.TargetMatch match = service.parse(null, TargetArgs.of("a1"));

        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> service.create(null, match, new TypedMapImpl())
        );
        assertTrue(e.getMessage().contains("--name"), e.getMessage());
        // a profile for a single launch always gets a name, it is never stored
        assertEquals("alpha-a1", service.createTransient(match, new TypedMapImpl()).name());
    }

    @Test
    void theSideOfANewProfileIsTheSideThatHasBeenParsed() {
        beta.withSide(Side.SERVER);

        Profile sideless = service.create("a", service.parse(null, TargetArgs.of("a1")), new TypedMapImpl());
        Profile server = service.create("b", service.parse(null, TargetArgs.of("b1")), new TypedMapImpl());

        assertEquals(Optional.empty(), sideless.side());
        assertEquals(Optional.of(Side.SERVER), server.side());
    }

    @Test
    void aStoredProfileForTheSameTargetIsFound() {
        Profile stored = profileService.save(profile("stored", alpha, "a1"));
        profileService.save(profile("other", alpha, "a2"));
        profileService.save(profile("other-target", beta, "a1"));

        assertEquals(Optional.of(stored), service.findProfile(service.parse(null, TargetArgs.of("a1"))));
        assertEquals(Optional.empty(), service.findProfile(service.parse(null, TargetArgs.of("a3"))));
    }

    @Test
    void aStoredProfileOfAnotherSideIsNotFound() {
        profileService.save(profile("stored", alpha, "a1").withSide(Optional.of(Side.SERVER)));

        assertEquals(Optional.empty(), service.findProfile(service.parse(null, TargetArgs.of("a1"))));
    }

    // resolving

    @Test
    void resolvingAStoredProfileSavesWhatChanged() {
        alpha.resolvingWith(profile -> profile.withJavaVersion(21));
        Profile stored = profileService.save(profile("stored", alpha, "a1"));

        Profile resolved = service.resolveAndPersist(stored, new TypedMapImpl());

        assertEquals(21, resolved.javaVersion());
        assertEquals(Optional.of(resolved), profileService.getProfile("stored"));
        assertEquals(List.of(stored), alpha.resolved());
    }

    @Test
    void resolvingWithoutChangesSavesNothing() {
        Profile stored = profile("stored", alpha, "a1");
        CountingProfileService counting = new CountingProfileService(root);
        counting.save(stored);
        LaunchTargetService service = new LaunchTargetServiceImpl(List.of(alpha), counting);

        assertSame(stored, service.resolveAndPersist(stored, new TypedMapImpl()));
        assertEquals(1, counting.saves);
    }

    @Test
    void resolvingAProfileThatIsNotStoredDoesNotStoreIt() {
        alpha.resolvingWith(profile -> profile.withJavaVersion(21));
        Profile transientProfile = profile("transient", alpha, "a1");

        Profile resolved = service.resolveAndPersist(transientProfile, new TypedMapImpl());

        // it is resolved, but no profile appears out of nowhere
        assertEquals(21, resolved.javaVersion());
        assertEquals(List.of(), profileService.getProfiles());
    }

    @Test
    void aChangedCopyOfAStoredProfileIsNotWrittenBack() {
        alpha.resolvingWith(profile -> profile.withJavaVersion(21));
        Profile stored = profileService.save(profile("stored", alpha, "a1"));
        // e.g. launched once with another target, or with additional jvm arguments
        Profile changed = stored.withVmArgs(List.of("-Xmx8G"));

        Profile resolved = service.resolveAndPersist(changed, new TypedMapImpl());

        assertEquals(21, resolved.javaVersion());
        assertEquals(Optional.of(stored), profileService.getProfile("stored"));
    }

    @Test
    void aChangeToAStoredProfileIsPersisted() {
        Profile stored = profileService.save(profile("stored", alpha, "a1"));

        Profile persisted = service.persist(stored, stored.withEulaStatus(EulaStatus.EXISTS));

        assertEquals(EulaStatus.EXISTS, persisted.eulaStatus());
        assertEquals(Optional.of(persisted), profileService.getProfile("stored"));
    }

    @Test
    void persistingWithoutAChangeSavesNothing() {
        Profile stored = profileService.save(profile("stored", alpha, "a1"));
        Profile equal = stored.withVmArgs(new ArrayList<>(stored.vmArgs()));

        // a saved profile is returned as it is read back, these are returned as they are
        assertSame(stored, service.persist(stored, stored));
        assertSame(equal, service.persist(stored, equal));
    }

    @Test
    void aProfileOfASingleLaunchIsNeverPersisted() {
        // created for a launch, never stored
        Profile once = profile("once", alpha, "a1");
        assertEquals(EulaStatus.EXISTS, service.persist(once, once.withEulaStatus(EulaStatus.EXISTS)).eulaStatus());
        assertEquals(Optional.empty(), profileService.getProfile("once"));

        // a stored profile with the options of a single launch: what is found out with it is not
        // written back, it would take the options with it
        Profile stored = profileService.save(profile("stored", alpha, "a1"));
        Profile launched = stored.withVmArgs(List.of("-Xmx8G"));
        assertEquals(
            List.of("-Xmx8G"), service.persist(launched, launched.withEulaStatus(EulaStatus.EXISTS)).vmArgs()
        );
        assertEquals(Optional.of(stored), profileService.getProfile("stored"));
    }

    @Test
    void resolveDoesNotSave() {
        alpha.resolvingWith(profile -> profile.withJavaVersion(21));
        Profile stored = profileService.save(profile("stored", alpha, "a1"));

        assertEquals(21, service.resolve(stored, new TypedMapImpl()).javaVersion());
        assertEquals(Optional.of(stored), profileService.getProfile("stored"));
    }

    @Test
    void aTargetMustNotChangeTheSideOfAProfile() {
        alpha.resolvingWith(profile -> profile.withSide(Optional.of(Side.CLIENT)));
        Profile stored = profileService.save(profile("stored", alpha, "a1"));

        LaunchException e = assertThrows(LaunchException.class, () -> service.resolveAndPersist(stored, new TypedMapImpl()));

        assertTrue(e.getMessage().contains("changed the side"), e.getMessage());
        assertEquals(Optional.of(stored), profileService.getProfile("stored"));
    }

    @Test
    void resolvingAndLaunchingAProfileWithoutItsTargetFails() {
        Profile profile = new Profile("p", Optional.empty(), new LaunchTargetHandle("from-a-plugin", "x"), root);

        assertThrows(LaunchException.class, () -> service.resolve(profile, new TypedMapImpl()));
        assertThrows(LaunchException.class, () -> service.resolveAndPersist(profile, new TypedMapImpl()));
        assertThrows(LaunchException.class, () -> service.launcher(profile, new TypedMapImpl()));
        assertThrows(LaunchException.class, () -> service.effective(profile));
    }

    @Test
    void launchingDelegatesToTheTarget() {
        Profile profile = profile("p", alpha, "a1");

        // the fake has no launcher
        assertThrows(LaunchException.class, () -> service.launcher(profile, new TypedMapImpl()));
        assertEquals(List.of(profile), alpha.launched());
        assertSame(profile, service.effective(profile));
    }

    private static final class CountingProfileService extends FakeProfileService {
        private int saves;

        private CountingProfileService(Path root) {
            super(root);
        }

        @Override
        public Profile save(Profile profile) {
            saves++;
            return super.save(profile);
        }
    }

}

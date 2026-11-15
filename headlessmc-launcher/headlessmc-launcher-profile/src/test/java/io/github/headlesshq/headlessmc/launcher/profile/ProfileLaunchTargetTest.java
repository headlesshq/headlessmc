package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A profile launching another profile inherits from it.
 */
class ProfileLaunchTargetTest {
    @TempDir
    Path root;

    private FakeProfileService profileService;
    private TestLaunchTargets targets;
    private LaunchTargetService service;
    private ProfileLaunchTarget target;
    private FakeLaunchTarget base;

    @BeforeEach
    void setup() {
        profileService = new FakeProfileService(root);
        targets = new TestLaunchTargets(profileService);
        service = targets.service();
        target = targets.profileTarget();
        base = new FakeLaunchTarget("base").withSide(Side.CLIENT);
        targets.with(target, base);
    }

    private Profile parent(String name) {
        return profileService.save(new Profile(name, Optional.of(Side.CLIENT), base.handle("x"), root.resolve(name)));
    }

    private Profile child(String name, String parent) {
        LaunchTargetService.TargetMatch match = service.parse(ProfileLaunchTarget.NAME, TargetArgs.of(parent));
        return profileService.save(service.create(name, match, new TypedMapImpl()));
    }

    // arguments

    @Test
    void parsesTheNameOfAnExistingProfile() {
        parent("main");

        ParsedTarget parsed = target.parse(TargetArgs.of("MAIN"));

        // the name as the profile has it
        assertEquals(new ProfileLaunchTarget.Handle("main"), target.handle(parsed.handle()));
        assertEquals(Optional.of(Side.CLIENT), parsed.side());
    }

    @Test
    void rejectsProfilesThatDoNotExistAndOtherArguments() {
        parent("main");

        assertTrue(assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of("nope")))
                       .getMessage().contains("no profile named 'nope'"));
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of()));
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of("main", "extra")));
    }

    @Test
    void isNeverDetected() {
        parent("main");

        // a profile only inherits from another profile if that has been asked for
        assertFalse(target.isDetectable());
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> service.parse(null, TargetArgs.of())
        );
        assertFalse(e.getMessage().contains(" - profile:"), e.getMessage());
    }

    @Test
    void completesProfileNamesForTheAllowedSides() {
        parent("main");
        profileService.save(new Profile("srv", VersionArg.parse("server", "1.21.1"), root.resolve("srv")));

        assertEquals(List.of("main", "srv"), values(target.complete(TargetArgs.of(""))));
        assertEquals(List.of("srv"), values(target.complete(new TargetArgs(List.of(""), Set.of(Side.SERVER)))));
        assertEquals(List.of(), values(target.complete(TargetArgs.of("main", ""))));
    }

    private static List<String> values(List<ArgCandidate> candidates) {
        return candidates.stream().map(ArgCandidate::value).toList();
    }

    @Test
    void hasNoDefaultName() {
        parent("main");

        ParsedTarget parsed = target.parse(TargetArgs.of("main"));

        assertEquals(Optional.empty(), target.defaultName(parsed.handle()));
        assertThrows(IllegalArgumentException.class, () -> service.create(
            null, new LaunchTargetService.TargetMatch(target, parsed), new TypedMapImpl()
        ));
    }

    // creating

    @Test
    void aChildGetsTheSideAndTheGameDirectoryOfItsParent() {
        Profile parent = parent("main");

        Profile child = child("debug", "main");

        assertEquals(parent.side(), child.side());
        assertEquals(parent.path(), child.path());
        assertEquals(ProfileLaunchTarget.NAME, child.target().name());
        assertEquals("profile:main", service.getLabel(child));
        assertEquals(Optional.of("main"), target.getParentName(child));
        assertEquals(Optional.empty(), target.getParentName(parent));
    }

    @Test
    void aChildCanHaveItsOwnGameDirectory() {
        parent("main");
        TypedMap options = new TypedMapImpl();
        options.put(LaunchTarget.GAME_DIR, root.resolve("own"));

        Profile child = service.create(
            "debug", service.parse(ProfileLaunchTarget.NAME, TargetArgs.of("main")), options
        );

        assertEquals(root.resolve("own"), child.path());
    }

    @Test
    void aChildOfAServerIsAServer() {
        profileService.save(new Profile("srv", Optional.of(Side.SERVER), base.handle("x"), root.resolve("srv")));

        assertEquals(Optional.of(Side.SERVER), child("srv-debug", "srv").side());
    }

    @Test
    void aChildOfAProfileWithoutSideHasNoSide() {
        profileService.save(new Profile("tool", Optional.empty(), base.handle("x"), root.resolve("tool")));

        assertEquals(Optional.empty(), child("tool-debug", "tool").side());
    }

    // inheriting

    @Test
    void theEffectiveProfileCombinesParentAndChild() {
        Map<String, String> parentProperties = new LinkedHashMap<>();
        parentProperties.put("shared", "parent");
        parentProperties.put("parent", "1");
        Profile parent = profileService.save(parent("main")
            .withVmArgs(List.of("-Xmx4G"))
            .withGameArgs(List.of("--parent"))
            .withSystemProperties(new LinkedHashMap<>(parentProperties))
            .withPatchers(List.of("log4j", "lwjgl"))
            .withOptions(new LaunchOptions(
                Optional.of(new LaunchOptions.Resolution(800, 600)), Optional.empty(),
                Optional.of(new LaunchOptions.Join(LaunchOptions.Join.Type.SERVER, "parent.example")), false
            ))
            .withJavaVersion(17)
            .withEulaStatus(EulaStatus.EXISTS)
            .withHasDefaultClientJvmArgs(true));

        Map<String, String> childProperties = new LinkedHashMap<>();
        childProperties.put("shared", "child");
        childProperties.put("child", "2");
        Profile child = child("debug", "main")
            .withPath(root.resolve("own"))
            .withVmArgs(List.of("-Ddebug=true"))
            .withGameArgs(List.of("--child"))
            .withSystemProperties(new LinkedHashMap<>(childProperties))
            .withPatchers(List.of("lwjgl", "paulscode"))
            .withOptions(new LaunchOptions(
                Optional.of(new LaunchOptions.Resolution(1920, 1080)), Optional.empty(), Optional.empty(), true
            ))
            .withEulaStatus(EulaStatus.NONE);

        Profile effective = target.effective(child);

        // of the child
        assertEquals("debug", effective.name());
        assertEquals(root.resolve("own"), effective.path());
        assertEquals(EulaStatus.NONE, effective.eulaStatus());
        // of the parent
        assertEquals(parent.target(), effective.target());
        assertTrue(effective.hasDefaultClientJvmArgs());
        // the parent, followed by the child
        assertEquals(List.of("-Xmx4G", "-Ddebug=true"), effective.vmArgs());
        assertEquals(List.of("--parent", "--child"), effective.gameArgs());
        assertEquals(List.of("log4j", "lwjgl", "paulscode"), effective.patchers());
        // the child replaces
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("shared", "child");
        properties.put("parent", "1");
        properties.put("child", "2");
        assertEquals(properties, effective.systemProperties());
        assertEquals(List.of("shared", "parent", "child"), List.copyOf(effective.systemProperties().keySet()));
        // options of the child, what it does not set comes from the parent
        assertEquals(Optional.of(new LaunchOptions.Resolution(1920, 1080)), effective.options().resolution());
        assertEquals(Optional.of("parent.example"), effective.options().join().map(LaunchOptions.Join::target));
        assertTrue(effective.options().demo());
        // the java version of the parent, as the child has none
        assertEquals(17, effective.javaVersion());
        assertEquals(21, target.effective(child.withJavaVersion(21)).javaVersion());
    }

    @Test
    void gameArgumentsStayNullIfNeitherHasAny() {
        profileService.save(parent("main").withGameArgs(null));
        Profile child = child("debug", "main").withGameArgs(null);

        assertNull(target.effective(child).gameArgs());
        assertEquals(List.of("--x"), target.effective(child.withGameArgs(List.of("--x"))).gameArgs());
    }

    @Test
    void inheritanceWorksOverMultipleLevels() {
        profileService.save(parent("main").withVmArgs(List.of("-Xmx4G")));
        profileService.save(child("middle", "main").withVmArgs(List.of("-Dmiddle")));
        Profile leaf = child("leaf", "middle").withVmArgs(List.of("-Dleaf"));

        Profile effective = target.effective(leaf);

        assertEquals("leaf", effective.name());
        assertEquals(base.getName(), effective.target().name());
        assertEquals(List.of("-Xmx4G", "-Dmiddle", "-Dleaf"), effective.vmArgs());
        assertEquals("profile:middle", service.getLabel(leaf));
    }

    @Test
    void aProfileWithAnotherTargetIsItsOwnEffectiveProfile() {
        Profile parent = parent("main");

        assertSame(parent, service.effective(parent));
    }

    // resolving and launching

    @Test
    void resolvingAChildResolvesItsParentAndSavesItThere() {
        base.resolvingWith(profile -> profile.withJavaVersion(21));
        Profile parent = parent("main");
        Profile child = child("debug", "main");

        Profile resolved = service.resolveAndPersist(child, new TypedMapImpl());

        // the child has nothing to remember, what has been resolved is in its parent
        assertSame(child, resolved);
        assertEquals(Optional.of(child), profileService.getProfile("debug"));
        assertEquals(List.of(parent), base.resolved());
        assertEquals(21, profileService.getProfile("main").orElseThrow().javaVersion());
        // and the child launches with it
        assertEquals(21, target.effective(resolved).javaVersion());
    }

    @Test
    void resolvingThroughMultipleLevelsSavesInTheRoot() {
        base.resolvingWith(profile -> profile.withJavaVersion(21));
        parent("main");
        Profile middle = profileService.save(child("middle", "main"));
        Profile leaf = child("leaf", "middle");

        service.resolveAndPersist(leaf, new TypedMapImpl());

        assertEquals(21, profileService.getProfile("main").orElseThrow().javaVersion());
        assertEquals(Optional.of(middle), profileService.getProfile("middle"));
        assertEquals(Optional.of(leaf), profileService.getProfile("leaf"));
    }

    @Test
    void theParentIsNotTreatedAsANewProfile() {
        List<Boolean> newProfile = new java.util.ArrayList<>();
        FakeLaunchTarget recording = new FakeLaunchTarget("recording") {
            @Override
            public Profile resolve(Profile profile, TypedMap options) {
                newProfile.add(options.get(NEW_PROFILE, false));
                return profile;
            }
        };
        targets.with(recording.withSide(Side.CLIENT));
        profileService.save(new Profile("main", Optional.of(Side.CLIENT), recording.handle("x"), root));
        Profile child = child("debug", "main");
        TypedMap options = new TypedMapImpl();
        options.put(LaunchTarget.NEW_PROFILE, true);
        options.put(LaunchTarget.UPDATE, true);

        service.resolve(child, options);

        // defaults for new profiles are for the child that is added, its parent already exists
        assertEquals(List.of(false), newProfile);
        // the options given by the caller are not changed
        assertTrue(options.get(LaunchTarget.NEW_PROFILE, false));
    }

    @Test
    void launchingAChildLaunchesTheEffectiveProfileWithTheTargetOfItsParent() {
        profileService.save(parent("main").withVmArgs(List.of("-Xmx4G")));
        Profile child = child("debug", "main").withVmArgs(List.of("-Ddebug"));

        // the fake target of the parent has no launcher
        assertThrows(LaunchException.class, () -> service.launcher(child, new TypedMapImpl()));

        assertEquals(1, base.launched().size());
        Profile launched = base.launched().getFirst();
        assertEquals("debug", launched.name());
        assertEquals(List.of("-Xmx4G", "-Ddebug"), launched.vmArgs());
    }

    @Test
    void describingAChildDescribesWhatItsRootLaunches() {
        FakePlatformService platformService = new FakePlatformService(
            new FakeVanillaPlatform("1.21.1"), FakePlatform.create("fabric", new String[]{"1.21.1", "0.16.9"})
        );
        targets.withVersions(platformService, root);
        Profile main = profileService.createDefault("main", VersionArg.parse("fabric", "1.21.1"));
        profileService.save(child("middle", "main"));
        Profile leaf = child("leaf", "middle");

        assertEquals(service.describe(main), service.describe(leaf));
        assertEquals(VersionArg.parse("fabric", "1.21.1"), service.requireVersion(leaf));
    }

    // broken inheritance

    @Test
    void aChildOfAMissingParentCanBeListedButNotLaunched() {
        parent("main");
        Profile child = child("debug", "main");
        profileService.remove(profileService.getProfile("main").orElseThrow());

        // it still is a profile with a side
        assertEquals(Optional.of(Side.CLIENT), child.side());
        assertEquals(TargetInfo.EMPTY, service.describe(child));
        assertEquals("profile:main", service.getLabel(child));
        for (Runnable action : List.<Runnable>of(
            () -> service.resolve(child, new TypedMapImpl()),
            () -> service.launcher(child, new TypedMapImpl()),
            () -> service.effective(child)
        )) {
            LaunchException e = assertThrows(LaunchException.class, action::run);
            assertTrue(e.getMessage().contains("launches profile main, which does not exist"), e.getMessage());
        }
    }

    @Test
    void aProfileCannotInheritFromItself() {
        parent("main");
        Profile child = child("debug", "main");
        // edited by hand: a profile launching itself
        Profile selfReferencing = profileService.save(target.withParent(child, "debug"));

        LaunchException e = assertThrows(LaunchException.class, () -> service.effective(selfReferencing));

        assertTrue(e.getMessage().contains("inherits from itself"), e.getMessage());
        assertThrows(LaunchException.class, () -> service.resolve(selfReferencing, new TypedMapImpl()));
        assertThrows(LaunchException.class, () -> service.launcher(selfReferencing, new TypedMapImpl()));
        // listing still works
        assertEquals(TargetInfo.EMPTY, service.describe(selfReferencing));
    }

    @Test
    void aCycleThroughAProfileThatIsNotSavedYetIsDetected() {
        parent("main");
        Profile a = child("a", "main");
        child("b", "a");
        // a is about to be changed to launch b, the stored a still launches main
        Profile edited = target.withParent(a, "b");

        LaunchException e = assertThrows(LaunchException.class, () -> service.effective(edited));

        assertTrue(e.getMessage().contains("inherits from itself"), e.getMessage());
        assertThrows(LaunchException.class, () -> service.resolve(edited, new TypedMapImpl()));
        // the stored profiles are fine
        assertDoesNotThrow(() -> service.effective(profileService.getProfile("b").orElseThrow()));
        assertEquals(0, base.resolved().size());
    }

    @Test
    void cyclesOverSeveralProfilesAreDetected() {
        parent("main");
        Profile a = child("a", "main");
        Profile b = child("b", "a");
        Profile c = child("c", "b");
        // a -> c -> b -> a
        Profile cyclicA = profileService.save(target.withParent(a, "c"));

        for (Profile profile : List.of(cyclicA, b, c)) {
            LaunchException e = assertThrows(LaunchException.class, () -> service.effective(profile));
            assertTrue(e.getMessage().contains("inherits from itself"), e.getMessage());
        }

        assertEquals(TargetInfo.EMPTY, service.describe(c));
        assertEquals(0, base.resolved().size());
    }

    @Test
    void aChildWhoseSideDiffersFromItsParentIsInvalid() {
        parent("main");
        // edited by hand, sides never change otherwise
        Profile child = child("debug", "main").withSide(Optional.of(Side.SERVER));

        LaunchException e = assertThrows(LaunchException.class, () -> service.resolve(child, new TypedMapImpl()));

        assertTrue(e.getMessage().contains("a server"), e.getMessage());
        assertTrue(e.getMessage().contains("a client"), e.getMessage());
        assertThrows(LaunchException.class, () -> service.launcher(child, new TypedMapImpl()));
        assertEquals(0, base.resolved().size());
    }

    // helpers for other code

    @Test
    void findsTheChildrenOfAProfile() {
        parent("main");
        parent("other");
        Profile a = child("a", "main");
        Profile b = child("b", "MAIN");
        child("c", "other");
        child("grandchild", "a");

        assertEquals(List.of(a, b), target.getChildren("main"));
        assertEquals(List.of(), target.getChildren("grandchild"));
    }

    @Test
    void aChildCanBeMovedToAnotherParent() {
        parent("main");
        Profile child = child("debug", "main");

        Profile moved = target.withParent(child, "renamed");

        assertEquals(Optional.of("renamed"), target.getParentName(moved));
        assertEquals(child.withTarget(moved.target()), moved);
    }

    @Test
    void theHandleSurvivesBeingStored(@TempDir Path dir) {
        ProfileTestFiles files = new ProfileTestFiles(dir);
        TestLaunchTargets stored = new TestLaunchTargets(files.service);
        ProfileLaunchTarget storedTarget = new ProfileLaunchTarget(stored.service(), files.service, files.jsonService);
        stored.with(storedTarget, base);
        files.service.save(new Profile("main", Optional.of(Side.CLIENT), base.handle("x"), dir.resolve("main")));
        Profile child = stored.service().create(
            "debug", stored.service().parse(ProfileLaunchTarget.NAME, TargetArgs.of("main")), new TypedMapImpl()
        );

        files.service.save(child);
        Profile loaded = files.service.getProfile("debug").orElseThrow();

        assertEquals(new ProfileLaunchTarget.Handle("main"), storedTarget.handle(loaded));
        assertEquals("main", stored.service().effective(loaded).target().name().equals("base")
            ? storedTarget.getParentName(loaded).orElseThrow() : "");
    }

}

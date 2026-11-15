package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.console.ArgSplitter;
import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.console.format.SimpleTableProvider;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.launcher.client.ClientTargetEnvironment;
import io.github.headlesshq.headlessmc.launcher.client.PackwizLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.client.VersionFileLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileMoveEvent;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileMovedEvent;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileMover;
import io.github.headlesshq.headlessmc.launcher.profile.TargetInfo;
import io.github.headlesshq.headlessmc.launcher.profile.TestEvent;
import io.github.headlesshq.headlessmc.launcher.profile.VersionHandle;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static io.github.headlesshq.headlessmc.launcher.client.ClientTargetEnvironment.LATEST_BUILD;
import static io.github.headlesshq.headlessmc.launcher.client.ClientTargetEnvironment.OLD_BUILD;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The profile commands with the real launch targets for clients, over two {@code .minecraft} directories:
 * adding profiles for other things than versions, moving, renaming, updating and editing what a profile launches.
 */
class ProfileTargetCommandsTest {
    @TempDir
    Path root;

    private final RecordingConsole console = new RecordingConsole();
    private final ArgSplitter splitter = line -> line.split(" ");
    private final TableProvider tables = new SimpleTableProvider();
    private final TestEvent<ProfileMoveEvent> moveEvent = new TestEvent<>();
    private final TestEvent<ProfileMovedEvent> movedEvent = new TestEvent<>();

    private FakeProfileService profileService;
    private ClientTargetEnvironment env;
    private LaunchTargetService launchTargetService;
    private ProfileLaunchTarget profileTarget;
    private ProfileMover mover;

    @BeforeEach
    void setup() {
        profileService = new FakeProfileService(root.resolve("profiles"));
        env = new ClientTargetEnvironment(root, profileService);
        launchTargetService = env.launchTargetService;
        profileTarget = (ProfileLaunchTarget) launchTargetService.require(ProfileLaunchTarget.NAME);
        mover = new ProfileMover(
            launchTargetService, profileTarget, profileService, env.mcFilesService, env.fileService, moveEvent, movedEvent
        );
    }

    /** {@code profile add} with a chainable {@code --name}. */
    private final class Add extends AddCommand {
        private Add() {
            super(launchTargetService, profileService, env.mcFilesService, env.fileService, console);
        }

        private Add withName(String name) {
            setName(name);
            return this;
        }
    }

    private Add add(String... targetArgs) {
        Add command = new Add();
        command.setTargetArgs(List.of(targetArgs));
        return command;
    }

    private MoveCommand move(String name) {
        MoveCommand command = new MoveCommand(
            launchTargetService, profileService, env.mcFilesService, mover, env.fileService, console
        );
        command.setName(name);
        return command;
    }

    private UpdateCommand update(String name) {
        UpdateCommand command = new UpdateCommand(
            launchTargetService, profileService, env.mcFilesService, env.fileService, console
        );
        command.setName(name);
        return command;
    }

    private EditCommand edit(String name, String field, String value) {
        EditCommand command = new EditCommand(
            new EditFieldHelper(launchTargetService, splitter), profileService, mover, tables, splitter, console
        );
        command.setName(name);
        command.setField(field);
        command.setValue(value);
        return command;
    }

    private Profile profile(String name) {
        return profileService.getProfile(name).orElseThrow();
    }

    private TargetInfo info(String name) {
        return launchTargetService.describe(profile(name));
    }

    private Path fabricJson(McFiles mcFiles, String build) {
        return env.json(mcFiles, "fabric-loader-" + build + "-1.21.1");
    }

    /** Adds a profile {@code main} for fabric 1.21.1 with a game directory containing a file. */
    private Profile addMain() throws IOException {
        add("fabric", "1.21.1").withName("main").run();
        Profile main = profile("main");
        Files.createDirectories(main.path());
        Files.writeString(main.path().resolve("options.txt"), "fov:70");
        return main;
    }

    /** A stored profile that has been resolved while 0.16.9 was the latest build. */
    private Profile addOutdated(String... specified) {
        Path json = env.install(env.primary, "fabric", "1.21.1", OLD_BUILD);
        Profile profile = new Profile("main", VersionArg.parse(specified), root.resolve("game")).withTarget(
            env.version.handle(new VersionHandle(
                VersionArg.parse(specified), VersionArg.parse("fabric", "1.21.1", OLD_BUILD), json
            ))
        );

        return profileService.save(profile);
    }

    // profile add

    @Test
    void addInstallsIntoTheRequestedMinecraftDirectory() {
        AddCommand command = add("fabric", "1.21.1");
        command.setMc(env.secondary.getMcDir().toString());
        command.run();

        Profile profile = profile("fabric-1.21.1");
        assertEquals(List.of(new ClientTargetEnvironment.Install(env.id("fabric", "1.21.1"), env.secondary)), env.installs);
        // pinned to the version in that directory, and its game directory is there as well
        assertEquals(Optional.of(fabricJson(env.secondary, LATEST_BUILD)), info("fabric-1.21.1").file());
        assertEquals(env.secondary.getMcDir().resolve("fabric-1.21.1"), profile.path());
        assertFalse(Files.exists(env.primary.getMcDir()));
        assertTrue(console.output().contains("Added profile fabric-1.21.1 for version fabric/1.21.1/" + LATEST_BUILD));
    }

    @Test
    void addWithoutMcUsesThePrimaryDirectory() {
        add("fabric", "1.21.1").run();

        assertEquals(Optional.of(fabricJson(env.primary, LATEST_BUILD)), info("fabric-1.21.1").file());
        assertEquals(env.primary.getMcDir().resolve("fabric-1.21.1"), profile("fabric-1.21.1").path());
    }

    @Test
    void aProfileForSomethingThatIsNotAVersionNeedsAName() {
        Path json = env.installCustom(env.secondary, "my-pack");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, add(json.toString())::run);

        assertEquals("Please specify a name for the profile with --name.", e.getMessage());
        assertEquals(List.of(), profileService.getProfiles());
    }

    @Test
    void addDetectsAVersionJson() {
        Path json = env.installCustom(env.secondary, "my-pack");

        add(json.toString()).withName("pack").run();

        Profile profile = profile("pack");
        assertEquals(VersionFileLaunchTarget.NAME, profile.target().name());
        assertEquals(Optional.of(Side.CLIENT), profile.side());
        assertEquals(Optional.of(json), info("pack").file());
        assertEquals(env.secondary.getMcDir().resolve("my-pack"), profile.path());
        // a new client profile gets the default jvm arguments, whatever it launches
        assertTrue(profile.hasDefaultClientJvmArgs());
        assertEquals(List.of(), env.installs);
        assertTrue(console.output().contains("Added profile pack for version-file:my-pack.json"), console.output());
    }

    @Test
    void addDetectsAPackwizPack() throws IOException {
        Path pack = Files.createDirectories(root.resolve("packs").resolve("my-pack"));
        Files.writeString(pack.resolve("pack.toml"), "[versions]\nminecraft = \"1.21.1\"\nfabric = \"" + OLD_BUILD + "\"\n");

        add(pack.toString(), "client").withName("pack").run();

        Profile profile = profile("pack");
        assertEquals(PackwizLaunchTarget.NAME, profile.target().name());
        assertEquals(pack, profile.path());
        // the version the pack asks for has been installed
        assertEquals(List.of(new ClientTargetEnvironment.Install(env.id("fabric", "1.21.1", OLD_BUILD), env.primary)), env.installs);
        assertEquals(Optional.of(fabricJson(env.primary, OLD_BUILD)), info("pack").file());
    }

    @Test
    void aProfileCanInheritFromAnotherProfile() throws IOException {
        Profile main = addMain();

        AddCommand command = add("main").withName("main-debug");
        command.setTarget(ProfileLaunchTarget.NAME);
        command.run();

        Profile child = profile("main-debug");
        assertEquals(ProfileLaunchTarget.NAME, child.target().name());
        assertEquals("profile:main", launchTargetService.getLabel(child));
        // the same game, with other settings
        assertEquals(main.side(), child.side());
        assertEquals(main.path(), child.path());
        assertEquals(1, env.installs.size());
        assertTrue(console.output().contains("Added profile main-debug for"), console.output());
    }

    @Test
    void theNameOfAProfileIsOnlyATargetIfTheProfileTargetIsNamed() throws IOException {
        addMain();

        // without --target profile, "main" is not something to launch
        assertThrows(IllegalArgumentException.class, add("main").withName("main-debug")::run);
        assertTrue(profileService.getProfile("main-debug").isEmpty());
    }

    @Test
    void anExplicitTargetHasToAcceptTheArguments() {
        AddCommand command = add("fabric", "1.21.1").withName("x");
        command.setTarget(VersionFileLaunchTarget.NAME);

        assertThrows(IllegalArgumentException.class, command::run);
        AddCommand unknown = add("fabric", "1.21.1");
        unknown.setTarget("nope");
        assertThrows(IllegalArgumentException.class, unknown::run);
        assertEquals(List.of(), profileService.getProfiles());
        assertEquals(List.of(), env.installs);
    }

    @Test
    void argumentsNoTargetCanLaunchAreRejectedBeforeAnythingIsInstalled() {
        assertThrows(IllegalArgumentException.class, add("fabric", "1.7.10")::run);
        assertThrows(IllegalArgumentException.class, add(root.resolve("missing.json").toString()).withName("x")::run);
        assertThrows(IllegalArgumentException.class, add("paper", "1.21.1")::run);

        assertEquals(List.of(), profileService.getProfiles());
        assertEquals(List.of(), env.installs);
    }

    // profile move

    @Test
    void moveRenamesAProfile() throws IOException {
        Profile main = addMain();

        MoveCommand command = move("main");
        command.setNewName("survival");
        command.run();

        assertTrue(profileService.getProfile("main").isEmpty());
        Profile renamed = profile("survival");
        // nothing but the name changed
        assertEquals(main, renamed.withName("main"));
        assertTrue(Files.exists(main.path().resolve("options.txt")));
        assertTrue(console.output().contains("Renamed profile main to survival"), console.output());
        assertEquals(1, movedEvent.fired().size());
        assertTrue(movedEvent.fired().getFirst().isRename());
    }

    @Test
    void moveMovesTheGameDirectory() throws IOException {
        Profile main = addMain();
        Path destination = root.resolve("games").resolve("main");

        MoveCommand command = move("main");
        command.setDirectory(destination.toString());
        command.run();

        assertEquals(destination, profile("main").path());
        assertEquals("fov:70", Files.readString(destination.resolve("options.txt")));
        assertFalse(Files.exists(main.path()));
        // what it launches is not in the game directory, it stays the same
        assertEquals(launchTargetService.describe(main), info("main"));
        assertTrue(console.output().contains("Moved profile main, moved " + main.path() + " to " + destination), console.output());
    }

    @Test
    void moveToAnotherMinecraftDirectoryMovesTheGameAndWhatItLaunches() throws IOException {
        Profile main = addMain();
        assertEquals(Optional.of(fabricJson(env.primary, LATEST_BUILD)), info("main").file());

        MoveCommand command = move("main");
        command.setMc(env.secondary.getMcDir().toString());
        command.run();

        // the game directory is where a profile added with --mc would have it
        Path destination = env.secondary.getMcDir().resolve("fabric-1.21.1");
        assertEquals(destination, profile("main").path());
        assertEquals("fov:70", Files.readString(destination.resolve("options.txt")));
        assertFalse(Files.exists(main.path()));
        // the same build has been installed there, and the profile launches it
        assertEquals(env.secondary, env.installs.getLast().mcFiles());
        assertEquals(Optional.of(fabricJson(env.secondary, LATEST_BUILD)), info("main").file());
        assertEquals(launchTargetService.describe(main).current(), info("main").current());
        assertTrue(
            console.output().contains("Profile main now launches " + fabricJson(env.secondary, LATEST_BUILD)),
            console.output()
        );
    }

    @Test
    void moveToAnotherMinecraftDirectoryWithItsOwnGameDirectory() throws IOException {
        addMain();
        Path destination = root.resolve("games").resolve("main");

        MoveCommand command = move("main");
        command.setMc(env.secondary.getMcDir().toString());
        command.setDirectory(destination.toString());
        command.setNewName("survival");
        command.run();

        assertEquals(destination, profile("survival").path());
        assertEquals(Optional.of(fabricJson(env.secondary, LATEST_BUILD)), info("survival").file());
        assertTrue(profileService.getProfile("main").isEmpty());
    }

    @Test
    void moveNeedsAProfileAndADestination() throws IOException {
        addMain();

        assertThrows(IllegalArgumentException.class, move(null)::run);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, move("main")::run);
        assertTrue(e.getMessage().contains("a directory, --mc <path> and/or --name <name>"), e.getMessage());
        MoveCommand unknown = move("nope");
        unknown.setNewName("x");
        assertThrows(IllegalArgumentException.class, unknown::run);
        // moving to where it already is
        MoveCommand same = move("main");
        same.setNewName("main");
        assertThrows(IllegalArgumentException.class, same::run);
    }

    @Test
    void aNameThatIsTakenNeedsForce() throws IOException {
        Profile main = addMain();
        add("1.21.1").withName("other").run();

        MoveCommand command = move("main");
        command.setNewName("other");
        assertThrows(IllegalArgumentException.class, command::run);
        assertEquals(main, profile("main"));

        command.setForce(true);
        command.run();

        assertTrue(profileService.getProfile("main").isEmpty());
        assertEquals(main.target(), profile("other").target());
    }

    @Test
    void profilesInheritingFromARenamedProfileFollowIt() throws IOException {
        addMain();
        AddCommand child = add("main").withName("main-debug");
        child.setTarget(ProfileLaunchTarget.NAME);
        child.run();

        MoveCommand command = move("main");
        command.setNewName("survival");
        command.run();

        assertEquals("profile:survival", launchTargetService.getLabel(profile("main-debug")));
        assertDoesNotThrow(() -> launchTargetService.effective(profile("main-debug")));
        assertTrue(console.output().contains("Updated profile main-debug"), console.output());
    }

    @Test
    void aSharedGameDirectoryIsOnlyMovedForAllProfilesUsingIt() throws IOException {
        Profile main = addMain();
        AddCommand child = add("main").withName("main-debug");
        child.setTarget(ProfileLaunchTarget.NAME);
        child.run();
        Path destination = root.resolve("games").resolve("main");

        MoveCommand command = move("main");
        command.setDirectory(destination.toString());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("main-debug"), e.getMessage());
        assertTrue(Files.exists(main.path().resolve("options.txt")));

        command.setWithShared(true);
        command.run();

        assertEquals(destination, profile("main").path());
        assertEquals(destination, profile("main-debug").path());
        assertTrue(Files.exists(destination.resolve("options.txt")));
    }

    @Test
    void anObserverCanPreventAMove() throws IOException {
        Profile main = addMain();
        // e.g. the game is running, see the ProfileMoveObserver of the backups
        moveEvent.observe(event -> {
            throw new IllegalStateException("profile " + event.profile().name() + " is running");
        });

        MoveCommand command = move("main");
        command.setDirectory(root.resolve("games").resolve("main").toString());

        assertThrows(IllegalStateException.class, command::run);
        assertEquals(main, profile("main"));
        assertTrue(Files.exists(main.path().resolve("options.txt")));
    }

    // profile update

    @Test
    void updateMovesAProfileForLatestToTheLatestBuild() {
        addOutdated("fabric", "1.21.1", "latest");

        update("main").run();

        TargetInfo info = info("main");
        assertEquals(Optional.of(VersionArg.parse("fabric", "1.21.1", "latest")), info.specified());
        assertEquals(Optional.of(VersionArg.parse("fabric", "1.21.1", LATEST_BUILD)), info.current());
        assertEquals(Optional.of(fabricJson(env.primary, LATEST_BUILD)), info.file());
        assertEquals(1, env.installs.size());
        assertTrue(console.output().contains(
            "Updated profile main: fabric/1.21.1/" + OLD_BUILD + " (" + fabricJson(env.primary, OLD_BUILD) + ") -> "
                + "fabric/1.21.1/" + LATEST_BUILD + " (" + fabricJson(env.primary, LATEST_BUILD) + ")"
        ), console.output());
    }

    @Test
    void launchingDoesNotUpdateButUpdateDoes() {
        Profile outdated = addOutdated("fabric", "1.21.1", "latest");

        // what a launch does
        Profile resolved = launchTargetService.resolveAndPersist(outdated, new TypedMapImpl());

        assertSame(outdated, resolved);
        assertEquals(List.of(), env.installs);
        update("main").run();
        assertNotEquals(outdated, profile("main"));
    }

    @Test
    void updateOfAnUpToDateProfileChangesNothing() {
        add("fabric", "1.21.1", "latest").withName("main").run();
        Profile before = profile("main");

        update("main").run();

        assertEquals(before, profile("main"));
        assertEquals(1, env.installs.size());
        assertTrue(console.output().contains("Profile main is up to date: fabric/1.21.1/" + LATEST_BUILD), console.output());
    }

    @Test
    void updateWithMcLaunchesTheProfileFromAnotherMinecraftDirectory() {
        Profile outdated = addOutdated("fabric", "1.21.1", OLD_BUILD);

        UpdateCommand command = update("main");
        command.setMc(env.secondary.getMcDir().toString());
        command.run();

        assertEquals(Optional.of(fabricJson(env.secondary, OLD_BUILD)), info("main").file());
        assertEquals(env.secondary, env.installs.getFirst().mcFiles());
        // unlike profile move, the game directory stays where it is
        assertEquals(outdated.path(), profile("main").path());
    }

    @Test
    void updateOfAProfileThatHasNothingToUpdate() {
        Path json = env.installCustom(env.secondary, "my-pack");
        add(json.toString()).withName("pack").run();
        Profile before = profile("pack");

        update("pack").run();

        assertEquals(before, profile("pack"));
        assertTrue(console.output().contains("Profile pack is up to date"), console.output());
    }

    @Test
    void updateNeedsAnExistingProfile() {
        assertThrows(IllegalArgumentException.class, update(null)::run);
        assertThrows(IllegalArgumentException.class, update("nope")::run);
    }

    // profile remove

    @Test
    void aProfileOthersInheritFromIsOnlyRemovedWithForce() throws IOException {
        addMain();
        AddCommand child = add("main").withName("main-debug");
        child.setTarget(ProfileLaunchTarget.NAME);
        child.run();
        RemoveCommand command = new RemoveCommand(profileTarget, profileService, console);
        command.setName("main");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("is launched by the profiles main-debug"), e.getMessage());
        assertTrue(e.getMessage().contains("--force"), e.getMessage());
        assertTrue(profileService.getProfile("main").isPresent());

        command.setForce(true);
        command.run();

        assertTrue(profileService.getProfile("main").isEmpty());
        // the child is still there, it just cannot be launched until it gets another target
        assertTrue(profileService.getProfile("main-debug").isPresent());
    }

    @Test
    void aProfileThatInheritsCanBeRemoved() throws IOException {
        addMain();
        AddCommand child = add("main").withName("main-debug");
        child.setTarget(ProfileLaunchTarget.NAME);
        child.run();

        RemoveCommand command = new RemoveCommand(profileTarget, profileService, console);
        command.setName("main-debug");
        command.run();

        assertEquals(List.of("main"), profileService.getProfiles().stream().map(Profile::name).toList());
    }

    // profile edit

    @Test
    void editingTheTargetForgetsWhatTheProfileHasBeenResolvedTo() throws IOException {
        Profile main = addMain();

        edit("main", "target", "fabric 1.21.1 " + OLD_BUILD).run();

        TargetInfo info = info("main");
        assertEquals(Optional.of(VersionArg.parse("fabric", "1.21.1", OLD_BUILD)), info.specified());
        assertEquals(Optional.empty(), info.current());
        assertEquals(Optional.empty(), info.file());
        // editing installs nothing, the next launch does
        assertEquals(1, env.installs.size());
        assertEquals(main.path(), profile("main").path());
    }

    @Test
    void theTargetOfAProfileCanBecomeAnotherKindOfTarget() throws IOException {
        addMain();
        Path json = env.installCustom(env.secondary, "my-pack");

        edit("main", "target", json.toString()).run();

        assertEquals(VersionFileLaunchTarget.NAME, profile("main").target().name());
        assertEquals(Optional.of(json), info("main").file());
        // "version" is a shorthand for profiles that launch a version, this one does not anymore
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, edit("main", "version", "1.21.1")::run);
        assertTrue(e.getMessage().contains("available: name, target, path"), e.getMessage());
    }

    @Test
    void theTargetCanBeNamedExplicitly() throws IOException {
        addMain();
        add("1.21.1").withName("vanilla").run();

        EditCommand command = edit("vanilla", "target", "main");
        command.setTarget(ProfileLaunchTarget.NAME);
        command.run();

        assertEquals("profile:main", launchTargetService.getLabel(profile("vanilla")));
    }

    @Test
    void aProfileCannotBeMadeToLaunchItselfThroughAnotherProfile() throws IOException {
        addMain();
        add("1.21.1").withName("vanilla").run();
        EditCommand command = edit("vanilla", "target", "main");
        command.setTarget(ProfileLaunchTarget.NAME);
        command.run();

        EditCommand self = edit("main", "target", "vanilla");
        self.setTarget(ProfileLaunchTarget.NAME);

        RuntimeException e = assertThrows(RuntimeException.class, self::run);
        assertTrue(e.getMessage().contains("inherits from itself"), e.getMessage());
        assertEquals(VersionHandle.VERSION, profile("main").target().name());
        // directly as well
        EditCommand direct = edit("main", "target", "main");
        direct.setTarget(ProfileLaunchTarget.NAME);
        assertThrows(RuntimeException.class, direct::run);
        assertEquals(VersionHandle.VERSION, profile("main").target().name());
    }

    @Test
    void theSideOfAProfileCannotBeChangedByEditingItsTarget() throws IOException {
        Profile main = addMain();

        assertThrows(IllegalArgumentException.class, edit("main", "target", "server paper 1.21.1")::run);
        assertThrows(IllegalArgumentException.class, edit("main", "target", "paper 1.21.1")::run);
        assertThrows(IllegalArgumentException.class, edit("main", "target", "nonsense")::run);

        assertEquals(main, profile("main"));
    }

    @Test
    void editingTheTargetToWhatItAlreadyIsKeepsThePin() throws IOException {
        Profile main = addMain();

        // the value the editor starts with is the label of the target
        edit("main", "target", launchTargetService.getLabel(main)).run();

        assertEquals(main, profile("main"));
    }

    @Test
    void editingTheJavaVersion() throws IOException {
        addMain();

        edit("main", "java", "17").run();
        assertEquals(17, profile("main").javaVersion());

        assertThrows(IllegalArgumentException.class, edit("main", "java", "seventeen")::run);
        assertEquals(17, profile("main").javaVersion());

        // an empty value: the java version the launched version asks for
        edit("main", "java", "").run();
        assertNull(profile("main").javaVersion());
    }

    @Test
    void editingTheNameRenamesLikeMove() throws IOException {
        addMain();
        AddCommand child = add("main").withName("main-debug");
        child.setTarget(ProfileLaunchTarget.NAME);
        child.run();

        edit("main", "name", "survival").run();

        assertTrue(profileService.getProfile("main").isEmpty());
        assertTrue(profileService.getProfile("survival").isPresent());
        assertEquals("profile:survival", launchTargetService.getLabel(profile("main-debug")));
        assertEquals(1, movedEvent.fired().size());
    }

    @Test
    void editingThePathOffersToMoveTheGameDirectory() throws IOException {
        Profile main = addMain();
        Path elsewhere = root.resolve("elsewhere");
        console.addInput("y");

        edit("main", "path", elsewhere.toString()).run();

        assertTrue(console.output().contains("Move the game directory there? [y/N]"), console.output());
        assertEquals(elsewhere, profile("main").path());
        assertEquals("fov:70", Files.readString(elsewhere.resolve("options.txt")));
        assertFalse(Files.exists(main.path()));
        assertEquals(1, movedEvent.fired().size());
        assertTrue(console.output().contains("Moved the game directory of profile main"), console.output());
    }

    @Test
    void decliningTheMoveOnlyChangesThePath() throws IOException {
        Profile main = addMain();
        Path elsewhere = root.resolve("elsewhere");
        // anything but yes, also no answer at all, e.g. if nobody can be asked
        console.addInput("");

        edit("main", "path", elsewhere.toString()).run();

        assertEquals(elsewhere, profile("main").path());
        assertTrue(Files.exists(main.path().resolve("options.txt")));
        assertFalse(Files.exists(elsewhere));
        assertEquals(List.of(), moveEvent.fired());
        assertTrue(console.output().contains("have not been moved. Use 'profile move main <directory>'"), console.output());
    }

    @Test
    void anExistingGameDirectoryIsUsedWithoutAsking() throws IOException {
        Profile main = addMain();
        // another game that already exists
        Path other = Files.createDirectories(root.resolve("other-game"));
        Files.writeString(other.resolve("options.txt"), "fov:110");
        console.addInput("y");

        edit("main", "path", other.toString()).run();

        assertFalse(console.output().contains("[y/N]"), console.output());
        assertEquals(other, profile("main").path());
        assertEquals("fov:110", Files.readString(other.resolve("options.txt")));
        assertTrue(Files.exists(main.path().resolve("options.txt")));
        assertEquals(List.of(), moveEvent.fired());
    }

    @Test
    void aProfileWithoutAGameDirectoryHasNothingToMove() {
        add("fabric", "1.21.1").withName("main").run();
        Path elsewhere = root.resolve("elsewhere");
        console.addInput("y");

        edit("main", "path", elsewhere.toString()).run();

        assertFalse(console.output().contains("[y/N]"), console.output());
        assertEquals(elsewhere, profile("main").path());
        assertEquals(List.of(), moveEvent.fired());
    }

    @Test
    void moveAndNoMoveAnswerTheQuestionInAdvance() throws IOException {
        Profile main = addMain();
        Path first = root.resolve("first");
        Path second = root.resolve("second");

        EditCommand noMove = edit("main", "path", first.toString());
        noMove.setMove(false);
        noMove.run();

        assertEquals(first, profile("main").path());
        assertTrue(Files.exists(main.path().resolve("options.txt")));

        // back to where the files are, then with --move
        edit("main", "path", main.path().toString()).run();
        EditCommand move = edit("main", "path", second.toString());
        move.setMove(true);
        move.run();

        assertEquals(second, profile("main").path());
        assertTrue(Files.exists(second.resolve("options.txt")));
        assertFalse(Files.exists(main.path()));
        assertFalse(console.output().contains("[y/N]"), console.output());
    }

    @Test
    void aMoveThatIsNotPossibleChangesNothing() throws IOException {
        Profile main = addMain();
        AddCommand child = add("main").withName("main-debug");
        child.setTarget(ProfileLaunchTarget.NAME);
        child.run();
        console.addInput("yes");

        // main-debug plays in the same game directory
        EditCommand command = edit("main", "path", root.resolve("elsewhere").toString());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);

        assertTrue(e.getMessage().contains("is also used by main-debug"), e.getMessage());
        assertTrue(e.getMessage().contains("profile move --help"), e.getMessage());
        assertEquals(main, profile("main"));
        assertTrue(Files.exists(main.path().resolve("options.txt")));
    }

    // profile list

    @Test
    void listShowsWhatEveryProfileLaunches() throws IOException {
        addMain();
        add(env.installCustom(env.secondary, "my-pack").toString()).withName("pack").run();
        AddCommand child = add("main").withName("main-debug");
        child.setTarget(ProfileLaunchTarget.NAME);
        child.run();
        // a profile that is neither a client nor a server
        profileService.save(profile("pack").withName("tool").withSide(Optional.empty()));

        new ListCommand(launchTargetService, profileService, tables, console).run();

        List<String> lines = console.output().lines().toList();
        String header = lines.stream().filter(line -> line.contains("target")).findFirst().orElseThrow();
        for (String column : List.of("name", "side", "target", "version", "current version", "java", "path")) {
            assertTrue(header.contains(column), header);
        }

        String main = row(lines, "main");
        assertTrue(main.contains("client"), main);
        assertTrue(main.contains("fabric/1.21.1"), main);
        assertTrue(main.contains("fabric/1.21.1/" + LATEST_BUILD), main);
        String pack = row(lines, "pack");
        assertTrue(pack.contains("version-file:my-pack.json"), pack);
        assertTrue(row(lines, "main-debug").contains("profile:main"), row(lines, "main-debug"));
        String tool = row(lines, "tool");
        assertFalse(tool.contains("client"), tool);
        assertFalse(tool.contains("server"), tool);
    }

    @Test
    void listShowsProfilesWhoseTargetIsNotAvailable() throws IOException {
        addMain();
        // e.g. the target of a plugin that has been removed
        env.targets.without(VersionHandle.VERSION);

        assertDoesNotThrow(() -> new ListCommand(launchTargetService, profileService, tables, console).run());

        assertTrue(row(console.output().lines().toList(), "main").contains("version"), console.output());
    }

    private static String row(List<String> lines, String name) {
        return lines.stream()
            .filter(line -> line.trim().startsWith(name + " ") || line.contains(" " + name + " "))
            .filter(line -> !line.contains("current version"))
            .filter(line -> !line.startsWith("Added "))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No row for " + name + " in\n" + String.join("\n", lines)));
    }

}

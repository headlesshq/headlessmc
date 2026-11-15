package io.github.headlesshq.headlessmc.commands.server;

import io.github.headlesshq.headlessmc.commands.util.LaunchTargetNameCompletions;
import io.github.headlesshq.headlessmc.console.ArgSplitter;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.FinalCommand;
import io.github.headlesshq.headlessmc.console.LongRunningCommand;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessBuilder;
import io.github.headlesshq.headlessmc.launcher.LifecycleService;
import io.github.headlesshq.headlessmc.launcher.ProcessLifecycle;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileResolver;
import io.github.headlesshq.headlessmc.launcher.server.Eula;
import io.github.headlesshq.headlessmc.launcher.server.EulaLauncher;
import io.github.headlesshq.headlessmc.launcher.server.ServerLauncher;
import io.github.headlesshq.headlessmc.launcher.server.ServerService;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.arg.Side;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Base class of all commands that launch a {@link Profile}.
 * What a profile launches is up to its {@link LaunchTarget}, this class takes care of everything around it.
 *
 * <p>A launch works with two profiles, which must not be mixed up:</p>
 * <ul>
 *     <li>the <em>stored</em> profile, {@link #getProfile()}, as it is in its file. Everything a launch finds out
 *     and wants to remember is a change to this profile: what the launch target resolved it to
 *     ({@link #resolve(Profile, TypedMap)}) and whether a server has a eula.txt
 *     ({@link #maybeEulaLaunch(Profile, Profile)}).</li>
 *     <li>the <em>launch</em> profile, {@link #prepareProfile(Profile)}: the stored profile with the options of
 *     this command applied, like additional jvm arguments. It is what is launched, and it is never saved.</li>
 * </ul>
 *
 * <p>The steps of a launch:</p>
 * <ol>
 *     <li>{@link #getProfile()} finds the profile to launch.</li>
 *     <li>{@link #checkOptions(Profile)} rejects options that make no sense for the profile,
 *     before anything is installed.</li>
 *     <li>{@link #beforeLaunch(Profile)} notifies observers, e.g. to back the profile up.</li>
 *     <li>The profile is resolved by its launch target, and saved if it is a stored profile.</li>
 *     <li>{@link #prepareProfile(Profile)} creates the launch profile.</li>
 *     <li>The launch target builds the process, which is launched by {@link #handle(ProcessLauncher)}.</li>
 * </ol>
 */
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public abstract class AbstractServerLaunchCommand implements Callable<Integer>, Runnable, FinalCommand, LongRunningCommand {
    private final LifecycleService lifecycleService;
    private final ProfileResolver profileResolver;
    private final PlatformService platformService;
    private final ServerLauncher serverLauncher;
    private final ServerService serverService;
    private final ArgSplitter splitter;
    private final Console console;
    private final LaunchTargetService launchTargetService;
    private final McFilesService mcFilesService;

    public abstract String getGameArgs();

    public abstract Profile getProfile();

    @CommandLine.Option(
        names = {"-t", "--target"},
        description = "The launch target that handles the arguments, e.g. version, server, version-file, jar, script, "
            + "packwiz or profile. If it is not specified, it is detected from the arguments.",
        completionCandidates = LaunchTargetNameCompletions.class
    )
    private @Nullable String target;

    @CommandLine.Option(
        names = {"--mc"},
        description = "The Minecraft directory to launch from and to install missing versions into, "
            + "if there are multiple (hmc.files.mc-dirs)."
    )
    private @Nullable String mc;

    @CommandLine.Option(
        names = {"-eula", "--eula-accept"},
        description = "Automatically accepts the EULA if needed.",
        defaultValue = "false"
    )
    private boolean acceptEula;

    @CommandLine.Option(
        names = {"-j", "--jvm"},
        description = "Arguments for the started JVM, e.g. --jvm \"-Xmx2G -Dproperty=value\""
    )
    private @Nullable String jvmArgs;

    @CommandLine.Option(names = {"--patchers"}, split = ",", description = "Comma-separated list of patchers to use.")
    private @Nullable List<String> patchers;

    @CommandLine.Option(
        names = {"--retries", "-ret"},
        description = "How many times to retry launching the process.",
        defaultValue = "0"
    )
    private int retries = 0;

    @CommandLine.Option(
        names = {"--no-backup"},
        description = "Does not back up the profile before launching, see hmc.backup.auto."
    )
    private boolean noBackup;

    @CommandLine.Option(
        names = {"--dry-run"},
        description = "Installs and downloads everything needed to launch, e.g. the jar, libraries, assets and the Java runtime, "
            + "but stops right before the game would be started."
    )
    private boolean dryRun;

    @Override
    public void run() {
        call();
    }

    @Override
    public Integer call() {
        Profile baseProfile = getProfile();
        checkOptions(baseProfile);
        beforeLaunch(baseProfile);
        TypedMap options = getTargetOptions();
        // everything that is saved is done with the profile as it is stored ...
        Profile stored = resolve(baseProfile, options);
        // ... and only what is launched gets the options of this launch
        Profile profile = prepareProfile(stored);
        maybeEulaLaunch(stored, profile);
        // TODO: patchers for server
        return handle(launchTargetService.launcher(profile, options));
    }

    /**
     * Notifies observers, e.g. automatic backups, that the given profile is about to be launched.
     * Call this before anything is installed or launched for the profile.
     *
     * @param profile the profile that is about to be launched.
     */
    protected void beforeLaunch(Profile profile) {
        if (dryRun) {
            return;
        }

        lifecycleService.beforeLaunch(profile, !noBackup);
    }

    /**
     * Lets the launch target of the profile find out what exactly is launched, installing it if needed.
     * If the profile is a stored profile, what has been found out is saved, so that the next launch launches the same.
     * Call this with the profile as it is stored, before {@link #prepareProfile(Profile)}.
     *
     * @param profile the profile to resolve.
     * @param options the options for the launch target, see {@link #getTargetOptions()}.
     * @return the resolved profile as it is stored now, still without the options of this launch.
     */
    protected Profile resolve(Profile profile, TypedMap options) {
        return launchTargetService.resolveAndPersist(profile, options);
    }

    /**
     * @return the options of this command for the launch target.
     */
    protected TypedMap getTargetOptions() {
        TypedMap options = new TypedMapImpl();
        String mc = this.mc;
        if (mc != null) {
            options.put(LaunchTarget.MC_FILES, mcFilesService.of(Paths.get(mc)));
        }

        return options;
    }

    /**
     * Finds the profile for the parameters of the commands that launch a profile by its name.
     * A name and {@code --target} exclude each other: without a launch target a single parameter is the name of
     * a stored profile. With a launch target, or with more than one parameter, <em>all</em> parameters,
     * including the first one, are what to launch, like for the {@code launch} command, see {@link ProfileResolver}.
     * A stored profile is never launched with another launch target than its own.
     *
     * @param name       the first parameter, the name of a profile if it is the only one.
     * @param targetArgs the parameters after the first one.
     * @param sides      the sides this command launches.
     * @param byName     finds the stored profile with a name.
     * @return the profile to launch.
     * @throws IllegalArgumentException if there is no profile for the parameters.
     */
    protected Profile getProfile(String name, List<String> targetArgs, Set<Side> sides, Function<String, Profile> byName)
        throws IllegalArgumentException {
        if (target == null && targetArgs.isEmpty()) {
            return byName.apply(name);
        }

        List<String> args = new ArrayList<>(targetArgs.size() + 1);
        args.add(name);
        args.addAll(targetArgs);
        return profileResolver.resolve(target, args, sides, getTargetOptions());
    }

    /**
     * Rejects options of this command that make no sense for the profile, instead of silently ignoring them.
     * This only depends on the {@link Profile#side() side} of the profile, which is known as soon as there is a
     * profile and never changes, so it is called before the profile is resolved:
     * a launch that is going to be rejected must not install or save anything first.
     *
     * @param profile the profile to launch, not resolved yet.
     * @throws IllegalArgumentException if an option cannot be used with the profile.
     */
    protected void checkOptions(Profile profile) throws IllegalArgumentException {
        // options for servers make no sense for anything that is not a server
        if (acceptEula && !profile.isServer()) {
            throw new IllegalArgumentException(
                "Profile " + profile.name() + " is not a server, --eula-accept cannot be used with it."
            );
        }
    }

    /**
     * Accepts the EULA of a server if that has been asked for. A server without a eula.txt is launched once
     * to create it, with the launch profile, the options of this launch apply to that launch as well.
     * Whether the server has a eula.txt is remembered in the stored profile.
     *
     * @param stored  the resolved profile as it is stored, without the options of this launch.
     * @param profile the profile that is launched.
     * @return the stored profile, with what has been found out about its eula.txt.
     */
    protected Profile maybeEulaLaunch(Profile stored, Profile profile) {
        if (!acceptEula || dryRun || !profile.isServer()) {
            return stored;
        }

        EulaLauncher.Result result = serverLauncher.getEulaLauncher().eulaLaunch(profile);
        result.eula().ifPresent(Eula::accept);
        return launchTargetService.persist(stored, stored.withEulaStatus(result.status()));
    }

    /**
     * Applies the arguments of this launch to a profile.
     * The resulting profile is only for this launch, it must not be saved: nothing that should be remembered
     * may be derived from it, see {@link LaunchTargetService#persist(Profile, Profile)}.
     *
     * @param baseProfile the resolved profile, as it is stored.
     * @return the profile to launch.
     */
    public Profile prepareProfile(Profile baseProfile) {
        String gameArgsArg = getGameArgs();
        List<String> parsedJvmArgs = new LinkedList<>(VmArgsUtil.parseArgs(splitter, this.jvmArgs));
        Map<String, @Nullable String> parsedSystemProperties = VmArgsUtil.removeSystemProperties(parsedJvmArgs.iterator());
        List<String> parsedGameArgs = VmArgsUtil.parseArgs(splitter, gameArgsArg);

        List<String> jvmArgs = new ArrayList<>(baseProfile.vmArgs());
        jvmArgs.addAll(parsedJvmArgs);

        SequencedMap<String, @Nullable String> systemProperties = new LinkedHashMap<>(baseProfile.systemProperties());
        systemProperties.putAll(parsedSystemProperties);

        // to make it completely right, game-args should override some of the provided ones
        List<String> gameArgs = new ArrayList<>(baseProfile.gameArgs() == null ? List.of() : baseProfile.gameArgs());
        gameArgs.addAll(parsedGameArgs);

        return baseProfile
            .withVmArgs(jvmArgs)
            .withGameArgs(gameArgs)
            .withSystemProperties(systemProperties);
    }

    /**
     * Launches the process of the given launcher. Creating the launcher has already installed and downloaded
     * everything the process needs, so a {@link #isDryRun() dry run} only resolves the Java runtime,
     * downloading it if necessary, and returns without starting the process.
     *
     * @param launcher the launcher to launch.
     * @return the exit code of the command.
     */
    protected Integer handle(ProcessLauncher launcher) {
        if (dryRun) {
            launcher.getJavaProcessBuilder().ifPresent(JavaProcessBuilder::resolveJava);
            log.info("Dry run: everything needed to launch {} has been downloaded, not launching.", launcher.getId());
            return 0;
        }

        ProcessLifecycle lifecycle = lifecycleService.wrap(launcher, retries);
        return lifecycle.call();
    }

    /**
     * A {@link #isDryRun() dry run} does not start the game, so it does not exit the interactive HeadlessMc shell.
     *
     * @return {@code false} if this is a dry run.
     */
    @Override
    public boolean isFinal() {
        return !dryRun;
    }

}

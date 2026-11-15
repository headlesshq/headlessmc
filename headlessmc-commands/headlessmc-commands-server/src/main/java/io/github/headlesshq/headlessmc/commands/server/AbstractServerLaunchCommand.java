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
import io.github.headlesshq.headlessmc.launcher.profile.ParsedTarget;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileResolver;
import io.github.headlesshq.headlessmc.launcher.profile.TargetArgs;
import io.github.headlesshq.headlessmc.launcher.server.Eula;
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

/**
 * Base class of all commands that launch a {@link Profile}.
 * What a profile launches is up to its {@link LaunchTarget}, this class takes care of everything around it:
 * <ol>
 *     <li>{@link #getProfile()} finds the profile to launch.</li>
 *     <li>{@link #beforeLaunch(Profile)} notifies observers, e.g. to back the profile up.</li>
 *     <li>The profile is resolved by its launch target. What the target finds out is saved in the profile,
 *     if the profile is a stored one.</li>
 *     <li>{@link #prepareProfile(Profile)} applies the options of this launch. This happens after resolving,
 *     so that they are never saved.</li>
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
        beforeLaunch(baseProfile);
        TypedMap options = getTargetOptions();
        Profile profile = prepareProfile(resolve(baseProfile, options));
        checkServerOptions(profile);
        maybeEulaLaunch(profile);
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
     * @return the resolved profile.
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
     * Lets the launch target given with {@code --target} parse arguments, to launch a stored profile
     * with something else than it usually launches, once. The profile keeps its side.
     *
     * @param profile the stored profile.
     * @param args    the arguments for the launch target.
     * @param sides   the sides this command launches.
     * @return the profile with the launch target for the arguments, which is not saved.
     */
    protected Profile withTarget(Profile profile, List<String> args, Set<Side> sides) {
        ParsedTarget parsed = launchTargetService.parse(target, new TargetArgs(args, sides)).parsed();
        if (!parsed.side().equals(profile.side())) {
            throw new IllegalArgumentException(
                "'" + String.join(" ", args) + "' cannot be launched with profile " + profile.name()
                    + ", it is for another side."
            );
        }

        return profile.withTarget(parsed.handle());
    }

    /**
     * Options for servers make no sense for anything that is not a server.
     * They are rejected, instead of silently doing nothing.
     */
    protected void checkServerOptions(Profile profile) {
        if (acceptEula && !profile.isServer()) {
            throw new IllegalArgumentException(
                "Profile " + profile.name() + " is not a server, --eula-accept cannot be used with it."
            );
        }
    }

    protected void maybeEulaLaunch(Profile profile) {
        if (acceptEula && !dryRun && profile.isServer()) {
            serverLauncher.getEulaLauncher().eulaLaunch(profile, serverService).ifPresent(Eula::accept);
        }
    }

    /**
     * Applies the arguments of this launch to a profile.
     * The resulting profile is only for this launch, it must not be saved.
     *
     * @param baseProfile the resolved profile.
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

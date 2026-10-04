package io.github.headlesshq.headlessmc.commands.server;

import io.github.headlesshq.headlessmc.console.ArgSplitter;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.FinalCommand;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.launcher.LifecycleService;
import io.github.headlesshq.headlessmc.launcher.ProcessLifecycle;
import io.github.headlesshq.headlessmc.java.args.ArgPair;
import io.github.headlesshq.headlessmc.java.args.SystemPropertyUtil;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileResolver;
import io.github.headlesshq.headlessmc.launcher.server.Eula;
import io.github.headlesshq.headlessmc.launcher.server.ServerLauncher;
import io.github.headlesshq.headlessmc.launcher.server.ServerService;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.util.*;
import java.util.concurrent.Callable;

@Getter
@Setter
@RequiredArgsConstructor
public abstract class AbstractServerLaunchCommand implements Callable<Integer>, Runnable, FinalCommand {
    private final LifecycleService lifecycleService;
    private final ProfileResolver profileResolver;
    private final PlatformService platformService;
    private final ServerLauncher serverLauncher;
    private final ServerService serverService;
    private final ArgSplitter splitter;
    private final Console console;
    private final McFiles mcFiles;

    public abstract String getGameArgs();

    public abstract Profile getProfile();

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

    @Override
    public void run() {
        call();
    }

    @Override
    public Integer call() {
        Profile profile = prepareProfile(getProfile());
        maybeEulaLaunch(profile);
        // TODO: patchers for server
        return handle(serverLauncher.launcher(profile));
    }

    protected void maybeEulaLaunch(Profile profile) {
        if (acceptEula) {
            serverLauncher.getEulaLauncher().eulaLaunch(profile, serverService).ifPresent(Eula::accept);
        }
    }

    public Profile prepareProfile(Profile baseProfileIn) {
        Profile baseProfile = baseProfileIn;
        if (baseProfile.side().isServer() && baseProfile.javaVersion() == null) {
            baseProfile = getServerLauncher().findJavaVersion(baseProfile);
            baseProfile = getServerService().save(baseProfile);
        }

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

    protected Integer handle(ProcessLauncher launcher) {
        ProcessLifecycle lifecycle = lifecycleService.wrap(launcher, retries);
        return lifecycle.call();
    }

}

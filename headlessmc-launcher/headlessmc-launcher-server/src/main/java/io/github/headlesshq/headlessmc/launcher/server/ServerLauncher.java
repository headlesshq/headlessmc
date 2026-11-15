package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.launcher.JavaFinder;
import io.github.headlesshq.headlessmc.java.launcher.JavaLauncherService;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessBuilder;
import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VanillaInstaller;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.util.typemap.Key;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

// TODO: patchers for server!
// TODO: fix log4j for servers?
//  shouldn't be tooo hard, depends on if paperclip and others e.g. fix hashes for the server.jar
//  but besides that log4j is either directly in the server.jar, or jar in jar
/**
 * Builds the processes of servers, and of everything else that is launched from a jar or a script.
 * What a profile launches is decided by its launch target, see {@link ServerLaunchTarget},
 * {@link JarLaunchTarget} and {@link ScriptLaunchTarget}, which use
 * {@link #launcher(Profile, Path, TypedMap)} with the executable they resolved.
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ServerLauncher {
    /**
     * The name of the process to launch, by default it is derived from the name of the profile.
     */
    public static final Key<String> PROCESS_NAME = new Key<>(String.class, "hmc.launch.server.process.name");
    /**
     * Whether to pipe the IO of the process instead of inheriting it, e.g. to stop a server launched for its eula.
     */
    public static final Key<Boolean> PIPE_IO = new Key<>(Boolean.class, "hmc.launch.server.pipe.io");

    private final JavaLauncherService javaLauncherService;
    private final LaunchTargetService launchTargetService;
    private final VanillaInstaller vanillaInstaller;
    private final PlatformService platformService;
    private final ProfileService profileService;
    private final JavaFinder javaFinder;
    private final ScriptLauncher scriptLauncher;

    /**
     * Launches a profile through its launch target.
     *
     * @param profile the profile to launch.
     * @return a launcher for the process of the profile.
     * @throws HeadlessMcException if the profile cannot be resolved or launched.
     */
    public ProcessLauncher launcher(Profile profile) throws HeadlessMcException {
        return launchProcess(profile, getProcessName(profile), false);
    }

    public EulaLauncher getEulaLauncher() {
        return new EulaLauncher(this);
    }

    /**
     * Finds the Java version for a server of the given version: the Java version of another profile for the
     * same Minecraft version, or else the one the Minecraft version requires.
     * This does not need the platform of the version.
     *
     * @param version the version of the server.
     * @return the Java version to launch the server with.
     * @throws HeadlessMcException if the Minecraft version cannot be found.
     */
    public int findJavaVersion(VersionArg version) throws HeadlessMcException {
        Integer java = profileService.getProfiles().stream()
            .filter(other -> launchTargetService.describe(other).version()
                .filter(otherVersion -> otherVersion.version().equals(version.version()))
                .isPresent())
            .map(Profile::javaVersion)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);

        if (java == null) {
            VersionID id = VersionID.resolve(platformService, version.asVanillaVersion());
            Version vanilla = vanillaInstaller.getVersion(id.getVersion());
            java = vanilla.requireJavaVersion();
        }

        return java;
    }

    /**
     * Resolves and launches a profile through its launch target, without saving what has been resolved.
     */
    ProcessLauncher launchProcess(Profile profile, String processName, boolean pipe) throws HeadlessMcException {
        TypedMap options = new TypedMapImpl();
        options.put(PROCESS_NAME, processName);
        options.put(PIPE_IO, pipe);
        return launchTargetService.launcher(launchTargetService.resolve(profile, options), options);
    }

    /**
     * Builds the process for an executable in the game directory of a profile.
     * A jar is launched with Java. Everything else is a script, which is run with the Java of the profile
     * first on the path, see {@link ScriptLauncher}.
     *
     * @param profile    the profile to launch, it has to have a {@link Profile#javaVersion()}.
     * @param executable the jar or script to launch.
     * @param options    {@link #PROCESS_NAME} and {@link #PIPE_IO}.
     * @return a launcher for the process.
     * @throws LaunchException if the profile has no Java version.
     */
    public ProcessLauncher launcher(Profile profile, Path executable, TypedMap options) throws LaunchException {
        Integer javaVersion = profile.javaVersion();
        if (javaVersion == null) {
            throw new LaunchException(
                "Profile " + profile.name() + " has no Java version, set it with 'profile edit "
                    + profile.name() + " java <version>'"
            );
        }

        String processName = options.get(PROCESS_NAME, getProcessName(profile));
        boolean pipe = options.get(PIPE_IO, false);
        Java java = javaFinder.findJava(javaVersion);
        JavaProcessBuilder builder = javaLauncherService.buildProcess()
            .id(processName)
            .pipeIO(pipe)
            .directory(profile.path())
            .java(java);

        if (executable.toString().toLowerCase(Locale.ENGLISH).endsWith(".jar")) {
            builder.jar(executable);
        } else if (scriptLauncher.isMockJava()) {
            scriptLauncher.captureJavaCommand(executable, profile.path()).applyTo(builder);
        } else {
            return scriptLauncher.launcher(profile, executable, java, processName, pipe);
        }

        builder.jvmArg(profile.vmArgs().toArray(String[]::new))
            .systemProperties(profile.systemProperties())
            .arg(profile.gameArgs() == null ? new String[0] : profile.gameArgs().toArray(String[]::new));

        return ProcessLauncher.of(builder, profile.path());
    }

    private static String getProcessName(Profile profile) {
        return (profile.isServer() ? "mc-server-" : "mc-") + profile.name();
    }

}

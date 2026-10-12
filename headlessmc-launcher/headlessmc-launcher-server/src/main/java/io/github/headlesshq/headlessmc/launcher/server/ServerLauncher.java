package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.launcher.JavaFinder;
import io.github.headlesshq.headlessmc.java.launcher.JavaLauncherService;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessBuilder;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VanillaInstaller;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.server.ServerSupport;
import io.github.headlesshq.headlessmc.version.Version;
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
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ServerLauncher {
    private final JavaLauncherService javaLauncherService;
    private final VanillaInstaller vanillaInstaller;
    private final PlatformService platformService;
    private final ProfileService profileService;
    private final ServerService serverService;
    private final JavaFinder javaFinder;
    private final ScriptLauncher scriptLauncher;

    public ProcessLauncher launcher(Profile profile) {
        return launchProcess(profile, "mc-server-" + profile.name(), false);
    }

    public EulaLauncher getEulaLauncher() {
        return new EulaLauncher(this);
    }

    public Profile findJavaVersion(Profile profile) {
        if (profile.javaVersion() != null) {
            return profile;
        }

        Integer java = profileService.getProfiles().stream()
            .filter(other -> other.currentVersion().version().equals(profile.currentVersion().version()))
            .map(Profile::javaVersion)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);

        if (java == null) {
            VersionID id = VersionID.resolve(platformService, profile.currentVersion());
            Version version = vanillaInstaller.getVersion(id.getVersion());
            java = version.requireJavaVersion();
        }

        return profile.withJavaVersion(java);
    }

    ProcessLauncher launchProcess(Profile profileIn, String processName, boolean pipe) {
        Profile profile = findJavaVersion(profileIn);
        VersionID id = VersionID.resolve(platformService, profile.currentVersion());
        Platform platform = id.getPlatform();
        ServerSupport serverSupport = platform.getServerSupport()
            .orElseThrow(() -> new IllegalStateException("Platform " + platform.getName() + " has no server support"));

        Path executable = serverSupport.serverFinder().findExecutable(profile.path());
        Java java = javaFinder.findJava(Objects.requireNonNull(profile.javaVersion()));
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

}

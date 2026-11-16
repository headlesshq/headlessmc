package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.AbstractVersionLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetHandle;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.VersionHandle;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.server.ServerInstallation;
import io.github.headlesshq.headlessmc.platform.server.ServerSupport;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * The {@value VersionHandle#SERVER} launch target: launches the server of a Minecraft version,
 * described by a {@link VersionArg}.
 *
 * <p>Arguments: {@code [server] [platform] <mc-version> [build]}, e.g. {@code server paper 1.21.1}.
 * The {@code server} is only optional for commands that launch nothing but servers, like {@code server add},
 * everywhere else a version without it is a client.</p>
 *
 * <p>A server is not installed into a {@code .minecraft} directory, but into the game directory of its profile.
 * What is pinned in the handle ({@link VersionHandle#currentTarget()}) is the executable of the server in that
 * directory, a jar or a script. With it a server can still be launched if its platform is not available anymore,
 * e.g. because the plugin providing it has been removed: a warning is logged and the pinned executable is launched.
 * </p>
 *
 * <p>Resolving a new profile ({@link #NEW_PROFILE}) installs the server, and takes the Java version and
 * jvm arguments the installer reports. Resolving an existing profile only finds the executable and,
 * if the profile has none, a Java version. A server that is not installed is not installed by launching it.</p>
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ServerLaunchTarget extends AbstractVersionLaunchTarget {
    private final PlatformService platformService;
    private final ServerLauncher serverLauncher;
    private final JsonService jsonService;
    private final AppFiles appFiles;

    @Override
    protected JsonService jsonService() {
        return jsonService;
    }

    @Override
    protected PlatformService platformService() {
        return platformService;
    }

    @Override
    protected Side getSide() {
        return Side.SERVER;
    }

    @Override
    protected Path defaultPath(String name, VersionHandle handle, TypedMap options) {
        return appFiles.getServerDir().resolve(name);
    }

    @Override
    protected List<String> defaultGameArgs() {
        // if server doesn't display a gui by default, maybe to-do --ui flag in launch command to remove this arg?
        return List.of("nogui");
    }

    @Override
    public Profile resolve(Profile profile, TypedMap options) throws HeadlessMcException {
        VersionHandle handle = validate(profile);
        Profile result = profile;
        VersionHandle resolved = handle;
        boolean update = options.get(UPDATE, false);
        if (options.get(NEW_PROFILE, false) || update) {
            VersionID id = resolveId(profile, update ? handle.version() : handle.effectiveVersion());
            ServerInstallation installation = getSupport(id.getPlatform()).installer()
                .installServer(id, profile.path(), options);

            // the build that has been installed, also if any build or the latest build has been asked for
            resolved = new VersionHandle(handle.version(), VersionHandle.normalize(installation.installed(id)), null);
            result = result.withJavaVersion(installation.javaVersion());
            if (!update) {
                result = result.withVmArgs(installation.vmArgs());
            }
        }

        resolved = resolved.withCurrentTarget(findExecutable(profile, resolved, true));
        if (resolved.current() == null) {
            resolved = resolved.withCurrent(resolved.version());
        }

        if (result.javaVersion() == null) {
            result = result.withJavaVersion(serverLauncher.findJavaVersion(resolved.effectiveVersion()));
        }

        if (!resolved.equals(handle)) {
            result = result.withTarget(handle(resolved));
        }

        return result;
    }

    @Override
    public ProcessLauncher launcher(Profile profile, TypedMap options) throws HeadlessMcException {
        VersionHandle handle = validate(profile);
        return serverLauncher.launcher(profile, findExecutable(profile, handle, false), options);
    }

    @Override
    public LaunchTargetHandle relocate(Profile profile, Path newPath) {
        VersionHandle handle = handle(profile);
        return handle(handle.withCurrentTarget(Relocation.relocate(handle.currentTarget(), profile.path(), newPath)));
    }

    /**
     * Finds the executable of the server. A pinned executable that exists is used as it is.
     * Otherwise the platform is asked to find it, which is not possible if the platform is not available.
     */
    private Path findExecutable(Profile profile, VersionHandle handle, boolean warn) throws LaunchException {
        @Nullable Path pinned = handle.currentTarget();
        boolean hasPin = pinned != null && Files.isRegularFile(pinned);
        VersionID id;
        try {
            id = VersionID.resolve(platformService, handle.effectiveVersion());
        } catch (HeadlessMcException | IllegalArgumentException e) {
            if (hasPin) {
                if (warn) {
                    log.warn("Version {} of profile {} cannot be resolved ({}), is a plugin missing? "
                                 + "Launching the pinned {} without its platform.",
                             handle.effectiveVersion(), profile.name(), e.getMessage(), pinned);
                }

                return pinned;
            }

            throw new LaunchException(
                "Failed to resolve version " + handle.effectiveVersion() + " of profile " + profile.name() + ": "
                    + e.getMessage() + ". If its platform is provided by a plugin, is the plugin still installed?"
                    + " The profile is not pinned to an installed server that could be launched instead.", e
            );
        }

        if (hasPin) {
            return pinned;
        }

        try {
            return getSupport(id.getPlatform()).serverFinder().findExecutable(profile.path()).toAbsolutePath().normalize();
        } catch (HeadlessMcException e) {
            throw new LaunchException(
                "Failed to find the server of profile " + profile.name() + " in " + profile.path() + ": "
                    + e.getMessage() + ". Is it installed? Install a server with 'server add "
                    + handle.version().withSide(null).toString(" ") + "'.", e
            );
        }
    }

    private VersionID resolveId(Profile profile, VersionArg arg) throws LaunchException {
        try {
            return VersionID.resolve(platformService, arg);
        } catch (HeadlessMcException | IllegalArgumentException e) {
            throw new LaunchException(
                "Failed to resolve version " + arg + " of profile " + profile.name() + ": " + e.getMessage(), e
            );
        }
    }

    private static ServerSupport getSupport(Platform platform) throws LaunchException {
        return platform.getServerSupport().orElseThrow(
            () -> new LaunchException("Platform " + platform.getName() + " has no server support")
        );
    }

}

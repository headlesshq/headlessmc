package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.launcher.LauncherConfig;
import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.AbstractVersionLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.GameDirService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.VersionHandle;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.client.ClientInstaller;
import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
import io.github.headlesshq.headlessmc.platform.client.VersionMatcherService;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.github.headlesshq.headlessmc.version.service.VersionJsonService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The {@value VersionHandle#VERSION} launch target: launches the client of a Minecraft version,
 * described by a {@link VersionArg}.
 *
 * <p>Arguments: {@code [client] [platform] <mc-version> [build]}, e.g. {@code 1.21.1},
 * {@code fabric 1.21.1} or {@code fabric 1.21.1 latest}.</p>
 *
 * <p>The handle ({@link VersionHandle}) keeps three things apart:</p>
 * <ul>
 *     <li>the version that has been <em>specified</em>, which can be vague, e.g. {@code fabric/26.3/latest},</li>
 *     <li>the <em>current</em> version it has been resolved to, e.g. {@code fabric/26.3/0.19.5},</li>
 *     <li>the version.json that version is installed as, the <em>pin</em>.</li>
 * </ul>
 * <p>The pin makes launches reproducible and independent of the platform. Once a profile is pinned it launches
 * that version.json, even if a newer build is released, if the version is installed in another
 * {@code .minecraft} directory as well, or if the platform of the version is not available anymore,
 * e.g. because the plugin providing it has been removed. In that case a warning is logged, and the
 * version.json is launched without the knowledge of its platform, see
 * {@link ClientLauncher#launcher(Profile, Account, Version, VersionID)}.</p>
 *
 * <p>The pin only moves if</p>
 * <ul>
 *     <li>the pinned version.json does not exist anymore. If its whole {@code .minecraft} directory is gone
 *     the disk is probably not mounted, resolving fails then, instead of installing everything somewhere else.</li>
 *     <li>the profile is resolved with {@link #UPDATE}.</li>
 *     <li>the specified version of the profile is edited.</li>
 * </ul>
 *
 * <p>A version that is not installed is installed into the directory given by {@link #MC_FILES},
 * else the directory of the old pin, else the primary directory.</p>
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class VersionLaunchTarget extends AbstractVersionLaunchTarget {
    private final VersionMatcherService versionMatcherService;
    private final VersionJsonService versionJsonService;
    private final PlatformService platformService;
    private final McFilesService mcFilesService;
    private final GameDirService gameDirService;
    private final ClientLauncher clientLauncher;
    private final DefaultJvmArgsService defaultJvmArgsService;
    private final LauncherConfig config;
    private final JsonService jsonService;
    /**
     * What the last profile has been resolved to. A launch resolves a profile and then builds its process,
     * this saves finding and reading the version a second time.
     */
    private final AtomicReference<@Nullable Resolved> lastResolved = new AtomicReference<>();

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
        return Side.CLIENT;
    }

    @Override
    protected Path defaultPath(String name, VersionHandle handle, TypedMap options) {
        return gameDirService.getGameDir(handle.version(), options.get(MC_FILES, mcFilesService.getPrimary()));
    }

    @Override
    public Profile resolve(Profile profile, TypedMap options) throws HeadlessMcException {
        VersionHandle handle = validate(profile);
        Resolved resolved = resolve(profile, handle, options, true);
        lastResolved.set(resolved);
        Profile result = resolved.handle().equals(handle) ? profile : profile.withTarget(handle(resolved.handle()));
        if (!resolved.handle().equals(handle)) {
            log.info("Profile {} launches {} ({})", profile.name(), resolved.handle().effectiveVersion(),
                     resolved.handle().currentTarget());
        }

        if (options.get(NEW_PROFILE, false)) {
            result = defaultJvmArgsService.withDefaultJvmArgs(result, resolved.version());
        }

        return result;
    }

    @Override
    public ProcessLauncher launcher(Profile profile, TypedMap options) throws HeadlessMcException {
        VersionHandle handle = validate(profile);
        Account account = options.get(ClientLauncher.ACCOUNT);
        if (account == null) {
            throw new LaunchException("No account to launch profile " + profile.name() + " with");
        }

        // the profile has been resolved, this only finds what it has been resolved to again
        @Nullable Resolved last = lastResolved.get();
        Resolved resolved = last != null && last.handle().equals(handle)
            ? last
            : resolve(profile, handle, options, false);
        return clientLauncher.launcher(profile, account, resolved.version(), resolved.id());
    }

    private Resolved resolve(Profile profile, VersionHandle handle, TypedMap options, boolean mayInstall) {
        boolean update = mayInstall && options.get(UPDATE, false);
        boolean forceInstall = mayInstall && options.get(ClientInstaller.FORCE_INSTALL, false);
        @Nullable McFiles requested = mayInstall ? options.get(MC_FILES) : null;
        @Nullable Path pin = handle.currentTarget();
        // a pin in another directory than the requested one is left, to move the profile to the requested directory
        boolean movesDir = pin != null && requested != null && !requested.isSame(mcFilesService.require(pin));
        if (pin != null && !update && !forceInstall && !movesDir) {
            if (Files.isRegularFile(pin)) {
                Version version = versionJsonService.getParser().parse(pin);
                return new Resolved(version, getId(profile, handle, version, mayInstall), handle);
            }

            failIfMcDirIsGone(profile, pin);
            log.info("Version {} of profile {} does not exist anymore, resolving {} again",
                     pin, profile.name(), handle.effectiveVersion());
        }

        VersionArg arg = update ? handle.version() : handle.effectiveVersion();
        VersionID id;
        try {
            id = VersionID.resolve(platformService, arg);
        } catch (HeadlessMcException | IllegalArgumentException e) {
            throw new LaunchException(
                "Failed to resolve version " + arg + " of profile " + profile.name() + ": " + e.getMessage()
                    + ". If its platform is provided by a plugin, is the plugin still installed?"
                    + " The profile is not pinned to an installed version that could be launched instead.", e
            );
        }

        List<Version> installed = requested == null
            ? versionJsonService.getInstalledVersions()
            : versionJsonService.getInstalledVersions(requested);
        Optional<VersionMatcherService.MatchResult> match = forceInstall
            ? Optional.empty()
            : versionMatcherService.match(id, installed, versionJsonService);

        Version version;
        VersionID resolvedId;
        if (match.isPresent()) {
            version = match.get().version();
            resolvedId = match.get().resolvedId();
        } else {
            if (!mayInstall) {
                throw new LaunchException("Version " + id + " of profile " + profile.name() + " is not installed.");
            }

            if (!config.autoInstall() && !options.get(NEW_PROFILE, false) && !update && !forceInstall) {
                throw new LaunchException(
                    "Failed to find installed version " + id + ". Consider installing it via \"headlessmc install "
                        + id.asArg().toString(" ") + "\"."
                );
            }

            version = install(id, getInstallDir(requested, pin), options);
            resolvedId = getInstalledId(id, version);
        }

        VersionHandle resolved = new VersionHandle(
            handle.version(),
            VersionHandle.normalize(resolvedId.asArg().withSide(null)),
            version.asFile().orElse(null)
        );

        return new Resolved(version, resolvedId, resolved);
    }

    /**
     * The id of a pinned version. If the platform of the profile is not available anymore,
     * the version can still be launched, but only with what the version.json itself tells us.
     */
    private @Nullable VersionID getId(Profile profile, VersionHandle handle, Version version, boolean warn) {
        try {
            return VersionID.resolve(platformService, handle.effectiveVersion());
        } catch (HeadlessMcException | IllegalArgumentException e) {
            // this is called for resolving and for launching, one warning is enough
            if (warn) {
                log.warn(
                    "Version {} of profile {} cannot be resolved ({}), is a plugin missing? Launching the pinned {} "
                        + "without its platform: libraries can only be downloaded from the vanilla repositories "
                        + "and a server can only be joined on launch if the version supports quick play.",
                    handle.effectiveVersion(), profile.name(), e.getMessage(), handle.currentTarget()
                );
            }

            log.debug("Failed to resolve {}", handle.effectiveVersion(), e);
            return clientLauncher.matchVersionId(version).orElse(null);
        }
    }

    private Version install(VersionID id, McFiles mcFiles, TypedMap options) {
        ClientSupport clientSupport = id.getPlatform().getClientSupport()
            .orElseThrow(() -> new LaunchException(
                id.getPlatform().getCapitalizedName() + " does not support clients."
            ));

        return clientSupport.installer().installClient(id, mcFiles, options);
    }

    /**
     * An id without a build, e.g. {@code fabric/1.21.1}, installs some build.
     * The installed version tells us which one.
     */
    private VersionID getInstalledId(VersionID id, Version version) {
        if (!id.isAnyBuild()) {
            return id;
        }

        try {
            Set<VersionID> ids = versionMatcherService.match(version, versionJsonService);
            return ids.stream().filter(id::isSamePlatformVersion).findFirst().orElse(id);
        } catch (HeadlessMcException e) {
            log.debug("Failed to match installed version {}", version.getId(), e);
            return id;
        }
    }

    private McFiles getInstallDir(@Nullable McFiles requested, @Nullable Path pin) {
        if (requested != null) {
            return requested;
        }

        if (pin != null) {
            McFiles pinned = mcFilesService.require(pin);
            if (pinned.isAvailable()) {
                return pinned;
            }
        }

        return mcFilesService.getPrimary();
    }

    /**
     * A version.json that is gone has been removed and can be installed again.
     * But if the whole {@code .minecraft} directory it was in is gone, the disk it is on is probably not mounted.
     * Installing the version, with all its libraries and assets, into another directory is not what the user wants.
     */
    private void failIfMcDirIsGone(Profile profile, Path pin) throws LaunchException {
        McFiles mcFiles = mcFilesService.require(pin);
        Path normalized = McFiles.normalize(pin);
        if (normalized.startsWith(McFiles.normalize(mcFiles.getMcDir())) && !mcFiles.isAvailable()) {
            throw new LaunchException(
                "Profile " + profile.name() + " launches " + pin + ", but the Minecraft directory "
                    + mcFiles.getMcDir() + " is not available, is its disk mounted? Use 'profile update "
                    + profile.name() + " --mc <path>' to move the profile to another Minecraft directory."
            );
        }
    }

    /**
     * @param version the version.json to launch.
     * @param id      the id of the version, {@code null} if it cannot be determined.
     * @param handle  the handle describing what has been resolved.
     */
    private record Resolved(Version version, @Nullable VersionID id, VersionHandle handle) {

    }

}

package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.auth.LastUsedAccountService;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.java.launcher.JavaLauncherService;
import io.github.headlesshq.headlessmc.launcher.LauncherConfig;
import io.github.headlesshq.headlessmc.launcher.args.ArgumentTemplateService;
import io.github.headlesshq.headlessmc.launcher.args.Arguments;
import io.github.headlesshq.headlessmc.launcher.args.ArgumentsService;
import io.github.headlesshq.headlessmc.launcher.assets.AssetDownloader;
import io.github.headlesshq.headlessmc.launcher.assets.AssetsLocation;
import io.github.headlesshq.headlessmc.launcher.libraries.ClasspathService;
import io.github.headlesshq.headlessmc.launcher.logging.LoggingDownloader;
import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchOptions;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.patcher.PatchResult;
import io.github.headlesshq.headlessmc.patcher.PatchService;
import io.github.headlesshq.headlessmc.patcher.Patcher;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.platform.VanillaVersionService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
import io.github.headlesshq.headlessmc.platform.client.VersionMatchException;
import io.github.headlesshq.headlessmc.platform.client.VersionMatcherService;
import io.github.headlesshq.headlessmc.util.maven.MavenRepository;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.ProcessedVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.service.VersionJsonService;
import io.github.headlesshq.headlessmc.version.util.Feature;
import io.github.headlesshq.headlessmc.version.util.Features;
import io.github.headlesshq.headlessmc.version.util.TemplateString;
import io.github.headlesshq.headlessmc.version.util.TemplateStrings;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ClientLauncher {
    /** The first version accepting {@code --server <host> --port <port>}. */
    static final String FIRST_SERVER_ARG_VERSION = "13w16a";
    /** The first version with quick play, which replaced {@code --server} and {@code --port}. */
    static final String FIRST_QUICK_PLAY_VERSION = "23w14a";

    private final ArgumentTemplateService argumentTemplateService;
    private final LastUsedAccountService lastUsedAccountService;
    private final SpecificsPropertiesService specificsPropertiesService;
    private final VersionMcFilesService versionMcFilesService;
    private final SpecificsDownloader specificsDownloader;
    private final VersionMatcherService versionMatcherService;
    private final VersionJsonService versionJsonService;
    private final TemplateStringService templateService;
    private final LoggingDownloader loggingDownloader;
    private final JavaLauncherService launcherService;
    private final ClasspathService classpathService;
    private final ArgumentsService argumentsService;
    private final AssetDownloader assetDownloader;
    private final PlatformService platformService;
    private final PatchService patchService;
    private final LauncherConfig config;

    /**
     * Launches the installed version matching {@link Profile#currentVersion()}.
     *
     * @param profile the profile to launch.
     * @param account the account to launch with.
     * @return a launcher for the client process.
     * @throws HeadlessMcException if the version cannot be found or something else goes wrong.
     */
    public ProcessLauncher launcher(Profile profile, Account account) throws HeadlessMcException {
        VersionID id = VersionID.resolve(platformService, profile.currentVersion());
        return launcher(profile, account, resolveVersion(id), id);
    }

    /**
     * Launches the given version.json, ignoring {@link Profile#currentVersion()}.
     * The version does not have to match a {@link VersionID}, e.g. a version installed by another launcher
     * under a custom name, without a parent. If it matches one, its platform is used like for
     * {@link #launcher(Profile, Account)}, otherwise libraries are resolved against the vanilla repositories
     * and joining a server on launch only works if the version.json declares quick play.
     *
     * @param profile the profile to launch.
     * @param account the account to launch with.
     * @param version the version.json to launch.
     * @return a launcher for the client process.
     * @throws HeadlessMcException if something goes wrong.
     */
    public ProcessLauncher launcher(Profile profile, Account account, Version version) throws HeadlessMcException {
        return launcher(profile, account, version, matchVersionId(version).orElse(null));
    }

    private ProcessLauncher launcher(
        Profile profile,
        Account account,
        Version resolvedVersion,
        @Nullable VersionID id
    ) throws HeadlessMcException {
        if (!Side.CLIENT.equals(profile.side())) {
            log.warn("Launching {} profile {} with client launcher", profile.side(), profile.name());
        }

        Path gameDir = profile.path();
        LaunchOptions options = profile.options();
        List<Patcher> patchers = getPatchers(profile);
        String name = id == null ? resolvedVersion.getId() : id.toString();

        // a version is launched with the files of a single .minecraft folder, e.g. ${library_directory} is one path
        McFiles mcFiles = versionMcFilesService.getMcFiles(resolvedVersion);
        log.debug("Launching {} from {}", name, mcFiles.getMcDir());
        ProcessedVersion version = versionJsonService.process(resolvedVersion);
        specificsDownloader.download(profile);

        // library overrides?
        // configuration???
        AssetsLocation assetsLocation = assetDownloader.download(mcFiles, version.getAssetIndex());

        TemplateStrings templates = templateService.create(mcFiles, assetsLocation, account, gameDir, version, options);
        Features features = Features.fromTemplates(templates);
        features.add(Feature.DEMO, options.demo());

        Optional<String> loggingArg = loggingDownloader.downloadLogging(mcFiles, templates, version);

        List<MavenRepository> repositories = new ArrayList<>(platformService.getVanillaPlatform().getRepositories());
        if (id != null) {
            repositories.addAll(id.getPlatform().getRepositories());
        }
        List<Path> unpatchedClasspath = classpathService.buildClasspath(mcFiles, templates, features, version, repositories);

        int javaVersion = profile.javaVersion() == null ? version.requireJavaVersion() : profile.javaVersion();
        PatchResult patchResult = new PatchResult(new LinkedHashSet<>(unpatchedClasspath), new LinkedHashSet<>());
        patchResult = patchService.patch(patchResult, javaVersion, patchers);

        templates.add(
            TemplateString.CLASSPATH,
            patchResult.files().stream()
                .map(Path::toAbsolutePath)
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator))
        );

        Arguments arguments = argumentsService.process(profile, version, features, loggingArg.orElse(null));
        arguments.systemProperties().putIfAbsent(
            "libraryDirectory",
            mcFiles.getLibraryDir().toAbsolutePath().toString()
        );

        // system properties required by the patchers, unless the user specified them otherwise
        patchResult.systemProperties().forEach(arguments.systemProperties()::putIfAbsent);
        specificsPropertiesService.getSystemProperties().forEach(arguments.systemProperties()::putIfAbsent);

        arguments = argumentTemplateService.process(arguments, templates);
        handleJoin(id, name, options, templates, arguments);

        List<String> jvmArgs = new ArrayList<>(arguments.vmArgs());
        for (Path javaAgent : patchResult.javaAgents()) {
            jvmArgs.add("-javaagent:" + javaAgent.toAbsolutePath());
        }

        return ProcessLauncher.of(
            launcherService.buildProcess()
                .id("mc-client-" + name)
                .version(javaVersion)
                .systemProperties(arguments.systemProperties())
                .jvmArg(jvmArgs.toArray(String[]::new))
                .arg(arguments.gameArgs().toArray(String[]::new))
                .mainClass(version.getMainClass())
                .directory(gameDir)
                .classpath(patchResult.files().toArray(Path[]::new))
                .classpathArgProvided(containsClasspathArg(jvmArgs)),
            gameDir
        );
    }

    /**
     * Finds the {@link VersionID} of a version.json launched directly, preferring a mod-loading platform
     * over vanilla, so that its repositories are used.
     */
    private Optional<VersionID> matchVersionId(Version version) {
        try {
            Set<VersionID> ids = versionMatcherService.match(version, versionJsonService);
            return ids.stream()
                .filter(id -> !id.isVanilla())
                .findFirst()
                .or(() -> ids.stream().findFirst());
        } catch (VersionMatchException e) {
            log.debug("Version {} does not match any platform", version.getId(), e);
            return Optional.empty();
        }
    }

    private Version resolveVersion(VersionID id) {
        Optional<VersionMatcherService.MatchResult> result = versionMatcherService.match(
            id, versionJsonService.getInstalledVersions(), versionJsonService
        );

        if (result.isEmpty()) {
            if (config.autoInstall()) {
                ClientSupport clientSupport = id.getPlatform().getClientSupport()
                    .orElseThrow(() -> new LaunchException(
                        id.getPlatform().getCapitalizedName() + " does not support clients."
                    ));

                McFiles mcFiles = versionMcFilesService.getInstallMcFiles(id);
                return clientSupport.installer().installClient(id, mcFiles.getMcDir(), new TypedMapImpl());
            }

            throw new LaunchException(
                "Failed to find installed version " + id + ". Consider installing it via \"headlessmc install "
                    + id.asArg().toString(" ") + "\"."
            );
        }

        return result.orElseThrow(() -> new LaunchException("Failed to get version for " + id)).version();
    }

    /**
     * Makes the game join the target of {@link LaunchOptions#join()}.
     * If the version.json declares the matching quick play argument, see {@link TemplateStringService},
     * nothing else has to be done. Otherwise, versions from {@value #FIRST_SERVER_ARG_VERSION}
     * until {@value #FIRST_QUICK_PLAY_VERSION} support joining servers via {@code --server <host> --port <port>},
     * which no version.json declares, so we add them. Older versions cannot join anything on launch.
     * If the id is unknown, we cannot tell the Minecraft version, so only quick play is supported.
     */
    private void handleJoin(
        @Nullable VersionID id,
        String name,
        LaunchOptions options,
        TemplateStrings templates,
        Arguments arguments
    ) {
        Optional<LaunchOptions.Join> join = options.join();
        if (join.isEmpty()) {
            return;
        }

        String target = join.get().target();
        switch (join.get().type()) {
            case SERVER -> handleServerJoin(id, name, target, templates, arguments);
            case SINGLEPLAYER -> warnIfUnsupported(name, templates, TemplateString.QUICK_PLAY_SINGLEPLAYER, target);
            case REALMS -> warnIfUnsupported(name, templates, TemplateString.QUICK_PLAY_REALMS, target);
        }
    }

    private void handleServerJoin(
        @Nullable VersionID id,
        String name,
        String server,
        TemplateStrings templates,
        Arguments arguments
    ) {
        if (templates.hasProcessed(TemplateString.QUICK_PLAY_MULTIPLAYER)) {
            return;
        }

        if (id == null) {
            log.warn("The version.json of {} does not support {} and its Minecraft version is unknown, not joining {}",
                name, TemplateString.QUICK_PLAY_MULTIPLAYER.getTemplateString(), server);
        } else if (isOlderThan(id, FIRST_SERVER_ARG_VERSION)) {
            log.warn("{} does not support joining a server on launch, not joining {}", id, server);
        } else if (isOlderThan(id, FIRST_QUICK_PLAY_VERSION)) {
            ServerAddress address = ServerAddress.parse(server);
            arguments.gameArgs().addAll(List.of("--server", address.host(), "--port", String.valueOf(address.port())));
        } else {
            log.warn("The version.json of {} does not support {}, not joining {}",
                id, TemplateString.QUICK_PLAY_MULTIPLAYER.getTemplateString(), server);
        }
    }

    private void warnIfUnsupported(String name, TemplateStrings templates, TemplateString quickPlay, String target) {
        if (!templates.hasProcessed(quickPlay)) {
            log.warn("The version.json of {} does not support {}, not joining {}", name, quickPlay.getTemplateString(), target);
        }
    }

    private boolean isOlderThan(VersionID id, String versionName) {
        VanillaVersionService versionService = platformService.getVanillaPlatform().getVersionService();
        VanillaVersion version = versionService.getVersion(versionName)
            .orElseThrow(() -> new IllegalStateException("Failed to find vanilla version " + versionName));
        return versionService.isOlderThan(id.getVersion(), version);
    }

    private static boolean containsClasspathArg(List<String> jvmArgs) {
        return jvmArgs.stream().anyMatch(arg -> arg.equals("-cp")
            || arg.equals("-classpath")
            || arg.equals("--class-path")
            || arg.startsWith("--class-path="));
    }

    private List<Patcher> getPatchers(Profile profile) {
        return profile.patchers().stream()
            .map(patchService::requirePatcher)
            .toList();
    }

    /**
     * A server address for the legacy {@code --server <host> --port <port>} arguments.
     *
     * @param host the host, IPv6 addresses without brackets.
     * @param port the port, {@value #DEFAULT_PORT} if none was specified.
     */
    @VisibleForTesting
    record ServerAddress(String host, int port) {
        static final int DEFAULT_PORT = 25565;

        /**
         * Parses {@code host}, {@code host:port}, {@code [ipv6]}, {@code [ipv6]:port} or an IPv6 address without port.
         *
         * @param address the address to parse.
         * @return the parsed address.
         * @throws LaunchException if the port is not a valid port.
         */
        static ServerAddress parse(String address) throws LaunchException {
            String trimmed = address.trim();
            String host = trimmed;
            String port = null;
            if (trimmed.startsWith("[") && trimmed.contains("]")) {
                int end = trimmed.indexOf(']');
                host = trimmed.substring(1, end);
                String rest = trimmed.substring(end + 1);
                if (rest.startsWith(":")) {
                    port = rest.substring(1);
                }
            } else if (trimmed.indexOf(':') != -1 && trimmed.indexOf(':') == trimmed.lastIndexOf(':')) {
                host = trimmed.substring(0, trimmed.indexOf(':'));
                port = trimmed.substring(trimmed.indexOf(':') + 1);
            }

            return new ServerAddress(host, port == null ? DEFAULT_PORT : parsePort(address, port));
        }

        private static int parsePort(String address, String port) throws LaunchException {
            int result;
            try {
                result = Integer.parseInt(port);
            } catch (NumberFormatException e) {
                throw new LaunchException("Invalid port in server address " + address, e);
            }

            if (result < 0 || result > 65535) {
                throw new LaunchException("Invalid port in server address " + address);
            }

            return result;
        }
    }

}

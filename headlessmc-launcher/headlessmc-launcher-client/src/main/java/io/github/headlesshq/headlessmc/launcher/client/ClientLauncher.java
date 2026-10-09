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
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ClientLauncher {
    private final ArgumentTemplateService argumentTemplateService;
    private final LastUsedAccountService lastUsedAccountService;
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
    private final McFiles mcFiles;

    public ProcessLauncher launcher(Profile profile, Account account) throws HeadlessMcException {
        if (!Side.CLIENT.equals(profile.side())) {
            log.warn("Launching {} profile {} with client launcher", profile.side(), profile.name());
        }

        Path gameDir = profile.path();
        LaunchOptions options = profile.options();
        List<Patcher> patchers = getPatchers(profile);

        VersionID id = VersionID.resolve(platformService, profile.currentVersion());
        Version resolvedVersion = resolveVersion(id);
        ProcessedVersion version = versionJsonService.process(resolvedVersion);

        // library overrides?
        // configuration???
        AssetsLocation assetsLocation = assetDownloader.download(version.getAssetIndex());

        TemplateStrings templates = templateService.create(assetsLocation, account, gameDir, version, options);
        Features features = Features.fromTemplates(templates);
        features.add(Feature.DEMO, options.demo());

        Optional<String> loggingArg = loggingDownloader.downloadLogging(templates, version);

        List<MavenRepository> repositories = new ArrayList<>(platformService.getVanillaPlatform().getRepositories());
        repositories.addAll(id.getPlatform().getRepositories());
        List<Path> unpatchedClasspath = classpathService.buildClasspath(templates, features, version, repositories);

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

        arguments = argumentTemplateService.process(arguments, templates);

        List<String> jvmArgs = new ArrayList<>(arguments.vmArgs());
        for (Path javaAgent : patchResult.javaAgents()) {
            jvmArgs.add("-javaagent:" + javaAgent.toAbsolutePath());
        }

        return ProcessLauncher.of(
            launcherService.buildProcess()
                .id("mc-client-" + id)
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

                return clientSupport.installer().installClient(id, mcFiles.getMcDir(), new TypedMapImpl());
            }

            throw new LaunchException(
                "Failed to find installed version " + id + ". Consider installing it via \"headlessmc install "
                    + id.asArg().toString(" ") + "\"."
            );
        }

        return result.orElseThrow(() -> new LaunchException("Failed to get version for " + id)).version();
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

}

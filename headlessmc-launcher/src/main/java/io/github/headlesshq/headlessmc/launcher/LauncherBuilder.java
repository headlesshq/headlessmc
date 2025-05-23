package io.github.headlesshq.headlessmc.launcher;

import io.github.headlesshq.headlessmc.launcher.auth.*;
import io.github.headlesshq.headlessmc.launcher.files.*;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.val;
import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.HeadlessMcImpl;
import io.github.headlesshq.headlessmc.api.classloading.Deencapsulator;
import io.github.headlesshq.headlessmc.api.command.CommandLineManager;
import io.github.headlesshq.headlessmc.api.exit.ExitManager;
import io.github.headlesshq.headlessmc.java.download.JavaDownloaderManager;
import io.github.headlesshq.headlessmc.jline.JLineCommandLineReader;
import io.github.headlesshq.headlessmc.jline.JLineProperties;
import io.github.headlesshq.headlessmc.launcher.command.LaunchContext;
import io.github.headlesshq.headlessmc.launcher.command.download.VersionInfoCache;
import io.github.headlesshq.headlessmc.launcher.download.ChecksumService;
import io.github.headlesshq.headlessmc.launcher.download.DownloadService;
import io.github.headlesshq.headlessmc.launcher.java.JavaService;
import io.github.headlesshq.headlessmc.launcher.launch.ProcessFactory;
import io.github.headlesshq.headlessmc.launcher.mods.ModManager;
import io.github.headlesshq.headlessmc.launcher.plugin.PluginManager;
import io.github.headlesshq.headlessmc.launcher.server.ServerManager;
import io.github.headlesshq.headlessmc.launcher.specifics.VersionSpecificModManager;
import io.github.headlesshq.headlessmc.launcher.specifics.VersionSpecificMods;
import io.github.headlesshq.headlessmc.launcher.util.UuidUtil;
import io.github.headlesshq.headlessmc.launcher.version.VersionService;
import io.github.headlesshq.headlessmc.logging.LoggingService;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.os.OSFactory;
import net.lenni0451.commons.httpclient.constants.Headers;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Helps with building and initializing {@link Launcher} instances.
 */
@Getter
@Setter
@CustomLog
@Accessors(fluent = true, chain = true)
public class LauncherBuilder {
    private DownloadService downloadService = new DownloadService();
    private Deencapsulator deencapsulator = new Deencapsulator();
    private LoggingService loggingService = new LoggingService();
    private ChecksumService sha1Service = new ChecksumService();
    private ExitManager exitManager = new ExitManager();
    private CommandLineManager commandLine = new CommandLineManager();
    private PluginManager pluginManager = new PluginManager();
    private VersionInfoCache versionInfoCache = new VersionInfoCache();

    private LauncherConfig launcherConfig;
    private FileManager fileManager;

    private HeadlessMc headlessMc;
    private VersionService versionService;
    private ProcessFactory processFactory;
    private ConfigService configService;
    private JavaService javaService;
    private AccountManager accountManager;
    private ModManager modManager;
    private VersionSpecificModManager versionSpecificModManager;
    private JavaDownloaderManager javaDownloaderManager;
    private ServerManager serverManager;

    private OS os;

    public LauncherBuilder initLogging() {
        if (!loggingService.isInitialized()) {
            loggingService.init();
        }

        return this;
    }

    public LauncherBuilder initFileManager() {
        return ifNull(LauncherBuilder::fileManager, LauncherBuilder::fileManager, () -> FileManager.mkdir(HeadlessMc.NAME));
    }

    public LauncherBuilder runAutoConfiguration() {
        AutoConfiguration.runAutoConfiguration(requireNonNull(this.fileManager, "FileManager not initialized"));
        return this;
    }

    public LauncherBuilder initConfigService() {
        return ifNull(LauncherBuilder::configService, LauncherBuilder::configService,
                () -> Service.refresh(new ConfigService(requireNonNull(this.fileManager, "FileManager not initialized"))));
    }

    public LauncherBuilder initHmcInstance() {
        return ifNull(LauncherBuilder::headlessMc, LauncherBuilder::headlessMc,
                () -> new HeadlessMcImpl(requireNonNull(configService, "ConfigHolder was null!"), commandLine, exitManager, loggingService));
    }

    public LauncherBuilder configureDownloadService() {
        if (requireNonNull(configService, "ConfigHolder was null!").getConfig().get(LauncherProperties.HTTP_USER_AGENT_ENABLED, true)) {
            downloadService.setHttpClientFactory(() ->
                    downloadService.getDefaultHttpClient()
                            .setHeader(Headers.USER_AGENT, configService.getConfig().get(LauncherProperties.HTTP_USER_AGENT, "Mozilla/5.0")));
        }

        return this;
    }

    public LauncherBuilder configureModDistributionService() {
        return ifNull(
                LauncherBuilder::modManager,
                LauncherBuilder::modManager,
                () -> ModManager.create(requireNonNull(downloadService(), "DownloadService not initialized")));
    }

    public LauncherBuilder initDefaultServices() {
        return ifNull(LauncherBuilder::os, LauncherBuilder::os, () -> OSFactory.detect(requireNonNull(configService, "ConfigHolder was null!").getConfig()))
                .ifNull(LauncherBuilder::launcherConfig, LauncherBuilder::launcherConfig, () -> {
                    FileManager mcFiles = MinecraftFinder.find(configService.getConfig(), os);
                    FileManager gameDir = FileManager.mkdir(configService.getConfig().get(LauncherProperties.GAME_DIR, mcFiles.getPath()));
                    return new LauncherConfig(configService, mcFiles, gameDir);
                })
                .ifNull(LauncherBuilder::versionService, LauncherBuilder::versionService, () -> new VersionService(requireNonNull(launcherConfig(), "LauncherConfig!")))
                .ifNull(LauncherBuilder::javaService, LauncherBuilder::javaService, () -> new JavaService(configService(), os()))
                .ifNull(LauncherBuilder::serverManager, LauncherBuilder::serverManager, () -> ServerManager.create(configService(), fileManager()));
    }

    public LauncherBuilder initAccountManager() throws AuthException {
        if (this.accountManager == null) {
            AccountStore accountStore = new AccountStore(requireNonNull(launcherConfig));
            this.accountManager = new AccountManager(new AccountValidator(), new OfflineChecker(requireNonNull(configService)), accountStore);
            accountManager.load(configService.getConfig());
        }

        return this;
    }

    public LauncherBuilder configureVersionSpecificModManager() {
        if (this.versionSpecificModManager == null) {
            requireNonNull(downloadService, "DownloadService was null!");
            requireNonNull(launcherConfig, "LauncherConfig was null!");
            this.versionSpecificModManager = new VersionSpecificModManager(downloadService, launcherConfig);
            versionSpecificModManager.addRepository(VersionSpecificMods.HMC_SPECIFICS);
            versionSpecificModManager.addRepository(VersionSpecificMods.MC_RUNTIME_TEST);
            versionSpecificModManager.addRepository(VersionSpecificMods.HMC_OPTIMIZATIONS);
        }

        return this;
    }

    public LauncherBuilder configureJavaDownloader() {
        if (this.javaDownloaderManager == null) {
            this.javaDownloaderManager = JavaDownloaderManager.getDefault();
        }

        return this;
    }

    public LauncherBuilder configureCommandLineProvider() {
        if (requireNonNull(configService, "ConfigHolder was null!").getConfig().get(JLineProperties.ENABLED, true)) {
            commandLine.setCommandLineProvider(JLineCommandLineReader::new);
        }

        return this;
    }

    public LauncherBuilder configureProcessFactory() {
        if (this.processFactory == null) {
            this.processFactory = new ProcessFactory(
                    requireNonNull(downloadService, "Download Service was null!"),
                    requireNonNull(launcherConfig, "LauncherConfig was null!"),
                    requireNonNull(os, "OS was null!")
            );
        }

        return this;
    }

    public LauncherBuilder deleteOldFiles() {
        if (requireNonNull(configService, "ConfigHolder was null!").getConfig().get(LauncherProperties.KEEP_FILES, false)) {
            return this;
        }

        for (val file : requireNonNull(launcherConfig, "LauncherConfig was null!").getFileManager().listFiles()) {
            if (file.isDirectory() && UuidUtil.isUuid(file.getName())) {
                try {
                    log.debug("Deleting " + file.getAbsolutePath());
                    fileManager.delete(file);
                } catch (IOException ioe) {
                    log.error("Couldn't delete " + file.getName() + " : " + ioe.getMessage());
                }
            }
        }

        return this;
    }

    public <T> LauncherBuilder ifNull(Function<LauncherBuilder, T> getter, BiConsumer<LauncherBuilder, T> setter, Supplier<T> def) {
        if (getter.apply(this) == null) {
            setter.accept(this, def.get());
        }

        return this;
    }

    public Launcher buildDefault() throws AuthException {
        return initLogging()
                .initFileManager()
                .runAutoConfiguration()
                .initConfigService()
                .initHmcInstance()
                .initDefaultServices()
                .initAccountManager()
                .configureDownloadService()
                .configureModDistributionService()
                .configureJavaDownloader()
                .configureVersionSpecificModManager()
                .configureCommandLineProvider()
                .configureProcessFactory()
                .deleteOldFiles()
                .buildAndConfigureCommandsAndPlugins();
    }

    public Launcher buildAndConfigureCommandsAndPlugins() {
        Launcher launcher = build();
        LauncherApi.setLauncher(launcher);
        LaunchContext launchContext = new LaunchContext(launcher);
        launcher.getCommandLine().setAllContexts(launchContext);
        launcher.getPluginManager().init(launcher);
        return launcher;
    }

    public Launcher build() {
        return new Launcher(
                requireNonNull(headlessMc, "HeadlessMc was null!"),
                requireNonNull(versionService, "VersionService was null!"),
                requireNonNull(launcherConfig, "LauncherConfig was null!"),
                requireNonNull(sha1Service, "Sha1Service was null!"),
                requireNonNull(downloadService, "Download Service was null"),
                requireNonNull(processFactory, "ProcessFactory was null!"),
                requireNonNull(configService, "ConfigService was null!"),
                requireNonNull(javaService, "JavaService was null!"),
                requireNonNull(accountManager, "AccountManager was null!"),
                requireNonNull(versionSpecificModManager, "VersionSpecificModManager was null!"),
                requireNonNull(pluginManager, "PluginManager was null!"),
                requireNonNull(javaDownloaderManager, "JavaDownloaderManager was null!"),
                requireNonNull(serverManager, "ServerManager was null!"),
                requireNonNull(versionInfoCache, "VersionInfoCache was null!"),
                requireNonNull(modManager, "ModDistributionPlatformManager was null!")
        );
    }

}

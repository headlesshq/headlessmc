package io.github.headlesshq.headlessmc.web.cheerpj.plugin;

import lombok.RequiredArgsConstructor;
import lombok.val;
import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.HeadlessMcImpl;
import io.github.headlesshq.headlessmc.api.command.Command;
import io.github.headlesshq.headlessmc.api.command.CommandContext;
import io.github.headlesshq.headlessmc.api.command.CopyContext;
import io.github.headlesshq.headlessmc.api.command.line.CommandLine;
import io.github.headlesshq.headlessmc.api.command.line.CommandLineReader;
import io.github.headlesshq.headlessmc.api.config.Config;
import io.github.headlesshq.headlessmc.api.config.ConfigImpl;
import io.github.headlesshq.headlessmc.api.config.HasConfig;
import io.github.headlesshq.headlessmc.api.exit.ExitManager;
import io.github.headlesshq.headlessmc.api.process.InAndOutProvider;
import io.github.headlesshq.headlessmc.auth.AbstractLoginCommand;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.download.JavaDownloaderManager;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.launcher.Service;
import io.github.headlesshq.headlessmc.launcher.auth.AccountManager;
import io.github.headlesshq.headlessmc.launcher.auth.AccountStore;
import io.github.headlesshq.headlessmc.launcher.auth.AccountValidator;
import io.github.headlesshq.headlessmc.launcher.auth.OfflineChecker;
import io.github.headlesshq.headlessmc.launcher.command.LaunchContext;
import io.github.headlesshq.headlessmc.launcher.command.download.VersionInfoCache;
import io.github.headlesshq.headlessmc.launcher.download.ChecksumService;
import io.github.headlesshq.headlessmc.launcher.download.DownloadService;
import io.github.headlesshq.headlessmc.launcher.files.ConfigService;
import io.github.headlesshq.headlessmc.launcher.files.FileManager;
import io.github.headlesshq.headlessmc.launcher.files.LauncherConfig;
import io.github.headlesshq.headlessmc.launcher.files.MinecraftFinder;
import io.github.headlesshq.headlessmc.launcher.java.JavaService;
import io.github.headlesshq.headlessmc.launcher.mods.ModManager;
import io.github.headlesshq.headlessmc.launcher.plugin.PluginManager;
import io.github.headlesshq.headlessmc.launcher.server.ServerManager;
import io.github.headlesshq.headlessmc.launcher.specifics.VersionSpecificModManager;
import io.github.headlesshq.headlessmc.launcher.specifics.VersionSpecificMods;
import io.github.headlesshq.headlessmc.launcher.util.UuidUtil;
import io.github.headlesshq.headlessmc.launcher.version.VersionService;
import io.github.headlesshq.headlessmc.launcher.version.VersionUtil;
import io.github.headlesshq.headlessmc.logging.Logger;
import io.github.headlesshq.headlessmc.logging.LoggerFactory;
import io.github.headlesshq.headlessmc.logging.LoggingService;
import io.github.headlesshq.headlessmc.logging.NoThreadFormatter;
import io.github.headlesshq.headlessmc.os.OSFactory;
import io.github.headlesshq.headlessmc.runtime.RuntimeProperties;
import io.github.headlesshq.headlessmc.runtime.commands.RuntimeContext;
import net.lenni0451.commons.httpclient.constants.ContentTypes;
import net.lenni0451.commons.httpclient.constants.Headers;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

@RequiredArgsConstructor
public class CheerpJLauncher {
    private static final UUID CACHE_UUID = UUID.fromString("e75fc20d-e629-4bf9-a236-c7acb4e9e0af");
    private final InAndOutProvider inAndOutProvider;
    private final CheerpJGUI gui;

    public void launch() {
        // https://cheerpj.com/docs/guides/File-System-support.html#files-mount-point
        Path root = Paths.get("").toAbsolutePath();
        Path headlessMcRoot = root.resolve("HeadlessMC");

        LoggingService loggingService = new LoggingService();
        loggingService.setPathFactory(() -> headlessMcRoot.resolve("headlessmc.log"));
        loggingService.setStreamFactory(() -> inAndOutProvider.getOut().get());
        loggingService.setFormatterFactory(NoThreadFormatter::new);
        loggingService.init();
        loggingService.setLevel(Level.INFO);

        Logger logger = LoggerFactory.getLogger("HeadlessMc");
        logger.info("Initializing HeadlessMc...");
        logger.info("This is just a demo!");
        logger.info("You can find the documentation at https://headlesshq.github.io/headlessmc");
        logger.info("Powered by CheerpJ!");
        logger.info("https://cheerpj.com/");
        logger.info("CheerpJ currently does not support some features we need, so you will probably not be able to launch the game yet!");
        logger.info("Display Size: " + gui.getFrame().getWidth() + "x" + gui.getFrame().getHeight());
        logger.warn("HeadlessMc is running in a browser and will not be able to make CORS requests.");
        logger.warn("You will need a CORS proxy or plugin.");

        initializeProperties(root);
        Config config = ConfigImpl.empty();
        HasConfig configs = () -> config;
        CommandLine commandLine = new CommandLine(inAndOutProvider, gui);
        HeadlessMc hmc = new HeadlessMcImpl(configs, commandLine, new ExitManager(), loggingService);
        hmc.getExitManager().setExitManager(i -> {
            if (i != 0) {
                logger.info("Error code " + i);
            }
        });

        try {
            initialize(hmc, logger, headlessMcRoot);
            logger.info("HeadlessMc initialized.");
        } catch (Throwable t) {
            logger.error("Failed to initialize HeadlessMc", t);
            CommandContext commands = new RuntimeContext(hmc);
            commandLine.setAllContexts(commands);
        }

        @SuppressWarnings("resource") // uses daemon thread factory and thread has lifetime of program
        ExecutorService service = Executors.newSingleThreadExecutor(CommandLineReader.DEFAULT_THREAD_FACTORY);
        gui.getCommandHandler().set(str -> service.submit(() -> {
            try {
                commandLine.getCommandConsumer().accept(str);
            } catch (Throwable t) {
                logger.error("Failed to execute command " + str, t);
            }
        }));
    }

    private void initializeProperties(Path root) {
        System.setProperty(RuntimeProperties.DONT_ASK_FOR_QUIT.getName(), "true");
        System.setProperty(LauncherProperties.MC_DIR.getName(), root.resolve("mc").toString());
        System.setProperty(LauncherProperties.GAME_DIR.getName(), root.resolve("mc").toString());
        System.setProperty(LauncherProperties.DUMMY_ASSETS.getName(), "true");
        System.setProperty(LauncherProperties.ASSETS_PARALLEL.getName(), "true");
        System.setProperty(LauncherProperties.EXTRACTED_FILE_CACHE_UUID.getName(), CACHE_UUID.toString());
        System.setProperty(LauncherProperties.ALWAYS_IN_MEMORY.getName(), "true");
        System.setProperty(LauncherProperties.IN_MEMORY_REQUIRE_CORRECT_JAVA.getName(), "false");
        // TODO: it the assets index file always corrupts on CheerpJ for some reason
        System.setProperty(LauncherProperties.ALWAYS_DOWNLOAD_ASSETS_INDEX.getName(), "true");
        System.setProperty(LauncherProperties.HTTP_USER_AGENT_ENABLED.getName(), "false");
    }

    private void initialize(HeadlessMc hmc, Logger logger, Path headlessMcRoot) {
        Security.addProvider(new BouncyCastleProvider());
        FileManager.setFactory(CheerpJFileManager::new);
        FileManager files = FileManager.mkdir(headlessMcRoot.toString());

        val configs = Service.refresh(new ConfigService(files));

        val os = OSFactory.detect(configs.getConfig());
        val mcFiles = MinecraftFinder.find(configs.getConfig(), os);
        val gameDir = FileManager.mkdir(configs.getConfig().get(LauncherProperties.GAME_DIR, mcFiles.getPath()));
        LauncherConfig launcherConfig = new LauncherConfig(configs, mcFiles, gameDir);
        val versions = new VersionService(launcherConfig);
        versions.setRetries(10);
        versions.refresh();

        val javas = Service.refresh(new JavaService(configs, os) {
            @Override
            public Java getCurrent() {
                return new Java("cheerpj", 8);
            }
        });

        val accountStore = new AccountStore(launcherConfig);
        val accounts = new AccountManager(new AccountValidator(), new OfflineChecker(configs), accountStore);
        // accounts.load(configs.getConfig()); // CheerpJ doesnt support logging in right now
        accounts.getOfflineChecker().setOffline(true);

        DownloadService downloadService = new DownloadService();
        val versionSpecificModManager = new VersionSpecificModManager(downloadService, launcherConfig);
        versionSpecificModManager.addRepository(VersionSpecificMods.HMC_SPECIFICS);
        versionSpecificModManager.addRepository(VersionSpecificMods.MC_RUNTIME_TEST);
        versionSpecificModManager.addRepository(VersionSpecificMods.HMC_OPTIMIZATIONS);

        VersionInfoCache versionInfoCache = new VersionInfoCache();
        val launcher = new Launcher(hmc, versions, launcherConfig,
                new ChecksumService(), downloadService,
                new CheerpJProcessFactory(downloadService, launcherConfig, os), configs,
                javas, accounts, versionSpecificModManager, new PluginManager(), JavaDownloaderManager.getDefault(),
                ServerManager.create(hmc, files), versionInfoCache,
                ModManager.create(downloadService));

        deleteOldFiles(launcher, logger);
        System.setProperty(LauncherProperties.KEEP_FILES.getName(), "true");

        LaunchContext launchContext = new LaunchContext(launcher);
        hmc.getCommandLine().setAllContexts(launchContext);
        CopyContext copyContext = new CopyContext(hmc, true);
        copyContext.add(new FilesCommand(launcher));
        copyContext.add(new ResizeCommand(hmc, gui));
        copyContext.add(new ClearCommand(hmc, gui));
        hmc.getCommandLine().setAllContexts(copyContext);
        for (Command command : copyContext) {
            if (command instanceof AbstractLoginCommand) {
                // The default HttpClient sets a user agent which causes the browser to make a CORS preflight check
                ((AbstractLoginCommand) command).setHttpClientFactory(() ->
                    downloadService.getDefaultHttpClient()
                        .setFollowRedirects(false)
                        .setHeader(Headers.ACCEPT, ContentTypes.APPLICATION_JSON.toString())
                        .setHeader(Headers.ACCEPT_LANGUAGE, "en-US,en"));
            }
        }

        // launcher.getPluginManager().init(launcher);
        launcher.getPluginManager().getPlugins().add(new CheerpJPlugin());
        hmc.log(VersionUtil.makeTable(VersionUtil.releases(versions.getContents())));
    }

    private void deleteOldFiles(Launcher launcher, Logger logger) {
        if (launcher.getConfig().get(LauncherProperties.KEEP_FILES, true)) {
            return;
        }

        for (val file : launcher.getLauncherConfig().getFileManager().listFiles()) {
            if (file.isDirectory() && !CACHE_UUID.toString().equals(file.getName()) && UuidUtil.isUuid(file.getName())) {
                try {
                    logger.debug("Deleting " + file.getAbsolutePath());
                    launcher.getLauncherConfig().getFileManager().delete(file);
                } catch (IOException ioe) {
                    logger.error("Couldn't delete " + file.getName(), ioe);
                }
            }
        }
    }

}

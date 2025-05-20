package io.github.headlesshq.headlessmc.launcher;

import io.github.headlesshq.headlessmc.api.command.line.CommandLineManager;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;
import io.github.headlesshq.headlessmc.api.HeadlessMcImpl;
import io.github.headlesshq.headlessmc.api.config.ConfigImpl;
import io.github.headlesshq.headlessmc.api.exit.ExitManager;
import io.github.headlesshq.headlessmc.java.download.JavaDownloaderManager;
import io.github.headlesshq.headlessmc.launcher.auth.AccountManager;
import io.github.headlesshq.headlessmc.launcher.auth.AccountStore;
import io.github.headlesshq.headlessmc.launcher.auth.AccountValidator;
import io.github.headlesshq.headlessmc.auth.ValidatedAccount;
import io.github.headlesshq.headlessmc.launcher.command.download.VersionInfoCache;
import io.github.headlesshq.headlessmc.launcher.download.ChecksumService;
import io.github.headlesshq.headlessmc.launcher.download.DownloadService;
import io.github.headlesshq.headlessmc.launcher.download.MockDownloadService;
import io.github.headlesshq.headlessmc.launcher.files.ConfigService;
import io.github.headlesshq.headlessmc.launcher.files.FileManager;
import io.github.headlesshq.headlessmc.launcher.files.LauncherConfig;
import io.github.headlesshq.headlessmc.launcher.java.JavaService;
import io.github.headlesshq.headlessmc.launcher.launch.MockProcessFactory;
import io.github.headlesshq.headlessmc.launcher.mods.ModManager;
import io.github.headlesshq.headlessmc.launcher.plugin.PluginManager;
import io.github.headlesshq.headlessmc.launcher.server.ServerManager;
import io.github.headlesshq.headlessmc.launcher.specifics.VersionSpecificModManager;
import io.github.headlesshq.headlessmc.launcher.version.VersionService;
import io.github.headlesshq.headlessmc.logging.LoggingService;
import io.github.headlesshq.headlessmc.os.OS;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class LauncherMock {
    public static final Launcher INSTANCE;

    static {
        INSTANCE = create();
    }

    @SneakyThrows
    public static Launcher create() {
        Path tempDir = Files.createTempDirectory("hmc-launcher-test");
        val base = FileManager.forPath(tempDir.toAbsolutePath().toString());
        val fileManager = base.createRelative("fileManager");
        val configs = new ConfigService(fileManager);
        configs.refresh();
        val in = new CommandLineManager();
        LoggingService loggingService = new LoggingService();
        val hmc = new HeadlessMcImpl(configs, in, new ExitManager(), loggingService);

        val os = new OS("windows", OS.Type.WINDOWS, "11", true);
        val mcFiles = base.createRelative("mcFiles");
        LauncherConfig launcherConfig = new LauncherConfig(configs, mcFiles, mcFiles);
        val versions = new VersionService(launcherConfig);
        val javas = new JavaService(configs, os);

        val store = new DummyAccountStore(launcherConfig);
        val accounts = new DummyAccountManager(store, new DummyAccountValidator());

        DownloadService downloadService = new MockDownloadService();
        val versionSpecificModManager = new VersionSpecificModManager(downloadService, launcherConfig);
        VersionInfoCache versionInfoCache = new VersionInfoCache();
        Launcher launcher = new Launcher(hmc, versions, launcherConfig,
                new ChecksumService(), new MockDownloadService(),
                new MockProcessFactory(downloadService, launcherConfig, os), configs,
                javas, accounts, versionSpecificModManager, new PluginManager(), new JavaDownloaderManager(),
                ServerManager.create(hmc, fileManager), versionInfoCache,
                ModManager.create(downloadService));

        launcher.getConfigService().setConfig(ConfigImpl.empty());

        return launcher;
    }

    private static final class DummyAccountManager extends AccountManager {
        public DummyAccountManager(AccountStore accountStore, AccountValidator validator) {
            super(validator, new TestOfflineChecker(), accountStore);
        }
    }

    public static final class DummyAccountValidator extends AccountValidator {
        public static final String DUMMY_XUID = "dummy-xuid";

        @Override
        public ValidatedAccount validate(StepFullJavaSession.FullJavaSession session) {
            return new ValidatedAccount(session, DUMMY_XUID);
        }
    }

    public static final class DummyAccountStore extends AccountStore {
        public DummyAccountStore(LauncherConfig launcherConfig) {
            super(launcherConfig);
        }

        @Override
        public List<ValidatedAccount> load() {
            return new ArrayList<>();
        }

        @Override
        public void save(List<ValidatedAccount> accounts) {
            // NOP
        }
    }

}

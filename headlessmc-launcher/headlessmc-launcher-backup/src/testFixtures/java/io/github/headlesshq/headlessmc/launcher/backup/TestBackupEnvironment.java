package io.github.headlesshq.headlessmc.launcher.backup;

import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.java.launcher.FakeJavaProcessManager;
import io.github.headlesshq.headlessmc.launcher.backup.profile.AutoBackupService;
import io.github.headlesshq.headlessmc.launcher.backup.profile.ProfileBackupProvider;
import io.github.headlesshq.headlessmc.launcher.backup.profile.ProfileUsageService;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileResolver;
import io.github.headlesshq.headlessmc.launcher.profile.WorldService;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;

import java.nio.file.Path;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Wires the backup services with the {@link ProfileBackupProvider} over a test directory.
 */
public class TestBackupEnvironment {
    /**
     * When the {@link #getClock() clock} starts.
     */
    public static final Instant START = Instant.parse("2026-10-28T12:30:00Z");

    private final TestConfig config = new TestConfig();
    private final TestClock clock = new TestClock(START);
    private final RecordingConsole console = new RecordingConsole();
    private final FakeJavaProcessManager processManager = new FakeJavaProcessManager();
    private final FileService fileService = new DefaultFileService(new DefaultFileSystemProvider());
    private final WorldService worldService = new WorldService();
    private final FakeProfileService profileService;
    private final BackupArchiveService archiveService;
    private final ProfileBackupProvider provider;
    private final BackupServiceImpl backupService;
    private final AutoBackupService autoBackupService;

    public TestBackupEnvironment(Path root, BackupProvider... additionalProviders) {
        profileService = new FakeProfileService(root.resolve("profiles"));
        archiveService = new BackupArchiveService(fileService);
        provider = new ProfileBackupProvider(
            new ProfileResolver(new FakePlatformService(new FakeVanillaPlatform("1.21.1")), profileService),
            profileService,
            worldService,
            new ProfileUsageService(processManager, worldService),
            archiveService
        );

        List<BackupProvider> providers = new ArrayList<>();
        providers.add(provider);
        providers.addAll(List.of(additionalProviders));
        backupService = new BackupServiceImpl(
            () -> config,
            new BackupProviderService(providers),
            archiveService,
            new DefaultJacksonJsonService(),
            fileService,
            TestFiles.appFiles(root),
            clock
        );

        autoBackupService = new AutoBackupService(() -> config, backupService, provider, worldService, console);
    }

    public TestConfig getConfig() {
        return config;
    }

    public TestClock getClock() {
        return clock;
    }

    public RecordingConsole getConsole() {
        return console;
    }

    public FakeJavaProcessManager getProcessManager() {
        return processManager;
    }

    public FileService getFileService() {
        return fileService;
    }

    public WorldService getWorldService() {
        return worldService;
    }

    public FakeProfileService getProfileService() {
        return profileService;
    }

    public BackupArchiveService getArchiveService() {
        return archiveService;
    }

    public ProfileBackupProvider getProvider() {
        return provider;
    }

    public BackupServiceImpl getBackupService() {
        return backupService;
    }

    public AutoBackupService getAutoBackupService() {
        return autoBackupService;
    }

    /**
     * A mutable {@link BackupConfig}.
     */
    public static class TestConfig implements BackupConfig {
        private Optional<String> dir = Optional.empty();
        private List<String> ignore = List.of("logs", "crash-reports");
        private String auto = "none";
        private int autoKeep = 5;

        public void setDir(Optional<String> dir) {
            this.dir = dir;
        }

        public void setIgnore(List<String> ignore) {
            this.ignore = ignore;
        }

        public void setAuto(String auto) {
            this.auto = auto;
        }

        public void setAutoKeep(int autoKeep) {
            this.autoKeep = autoKeep;
        }

        @Override
        public Optional<String> dir() {
            return dir;
        }

        @Override
        public List<String> ignore() {
            return ignore;
        }

        @Override
        public String auto() {
            return auto;
        }

        @Override
        public int autoKeep() {
            return autoKeep;
        }

    }

    /**
     * A {@link Clock} in UTC that only moves when {@link #advance(Duration) advanced}.
     */
    public static class TestClock extends Clock {
        private Instant now;

        public TestClock(Instant now) {
            this.now = now;
        }

        /**
         * @param duration how far to move this clock forward.
         */
        public void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

    }

}

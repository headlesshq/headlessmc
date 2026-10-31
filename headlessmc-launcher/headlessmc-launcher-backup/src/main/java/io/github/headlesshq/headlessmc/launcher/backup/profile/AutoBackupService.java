package io.github.headlesshq.headlessmc.launcher.backup.profile;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.launcher.backup.*;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileLaunchEvent;
import io.github.headlesshq.headlessmc.launcher.profile.WorldService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Backs up profiles before they are launched, as configured by {@link BackupConfig#auto()}.
 * No backup is created if nothing changed since the last backup of the same type,
 * or if the profile is already running. If creating the backup fails, the launch fails as well.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class AutoBackupService {
    /**
     * The value of {@link BackupConfig#auto()} that disables automatic backups.
     */
    public static final String NONE = "none";

    private final Holder<BackupConfig> config;
    private final BackupService backupService;
    private final ProfileBackupProvider provider;
    private final WorldService worldService;
    private final Console console;

    void onLaunch(@Observes ProfileLaunchEvent event) {
        if (event.autoBackup()) {
            backupBeforeLaunch(event.profile());
        }
    }

    /**
     * Backs up the given profile, if configured, before it is launched.
     *
     * @param profile the profile that is about to be launched.
     * @return the created backup, or an empty {@link Optional} if no backup was created.
     */
    public Optional<Backup> backupBeforeLaunch(Profile profile) {
        Optional<ProfileBackupType> type = getType();
        if (type.isEmpty() || !hasFiles(profile, type.get())) {
            return Optional.empty();
        }

        BackupRequest request = new BackupRequest(
            provider.getName(), profile.name(), profile, type.get().getName(), null,
            List.of(), true, BackupOrigin.LAUNCH, true
        );
        BackupPlan plan = provider.plan(request);
        Optional<String> usage = provider.getUsage(new BackupScope(plan.source(), plan.paths(), List.of()), profile);
        if (usage.isPresent()) {
            log.warn("Not backing up {} before launching: {}", profile.name(), usage.get());
            return Optional.empty();
        }

        if (isUnchanged(profile, type.get())) {
            log.info("Not backing up {} before launching, nothing changed since the last backup", profile.name());
            return Optional.empty();
        }

        try {
            Backup backup = backupService.create(request);
            console.write("Backed up " + profile.name() + " before launching: " + backup.id());
            return Optional.of(backup);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                "Failed to back up " + profile.name() + " before launching, launch with --no-backup to skip it: "
                    + e.getMessage(), e
            );
        }
    }

    private Optional<ProfileBackupType> getType() {
        String auto = config.get().auto().trim().toLowerCase(Locale.ENGLISH);
        if (auto.isEmpty() || NONE.equals(auto)) {
            return Optional.empty();
        }

        if (ProfileBackupType.FULL.getName().equals(auto)) {
            return Optional.of(ProfileBackupType.FULL);
        }

        if (ProfileBackupType.WORLDS.getName().equals(auto)) {
            return Optional.of(ProfileBackupType.WORLDS);
        }

        log.warn("Invalid value '{}' for {}.auto, expected none, worlds or full", auto, BackupConfig.PREFIX);
        return Optional.empty();
    }

    private boolean hasFiles(Profile profile, ProfileBackupType type) {
        if (!Files.isDirectory(profile.path())) {
            return false;
        }

        return type != ProfileBackupType.WORLDS || !worldService.listWorlds(profile).isEmpty();
    }

    private boolean isUnchanged(Profile profile, ProfileBackupType type) {
        return backupService.list(provider.getName(), profile.name()).stream()
            .filter(backup -> type.getName().equals(backup.type()))
            .filter(backup -> backup.source().toAbsolutePath().normalize().equals(profile.path().toAbsolutePath().normalize()))
            .max(Comparator.comparing(Backup::created))
            .map(backup -> backupService.diff(backup, false).isEmpty())
            .orElse(false);
    }

}

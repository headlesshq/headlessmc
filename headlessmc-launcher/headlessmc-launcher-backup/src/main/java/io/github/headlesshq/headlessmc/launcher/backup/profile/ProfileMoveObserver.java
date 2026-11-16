package io.github.headlesshq.headlessmc.launcher.backup.profile;

import io.github.headlesshq.headlessmc.launcher.backup.Backup;
import io.github.headlesshq.headlessmc.launcher.backup.BackupScope;
import io.github.headlesshq.headlessmc.launcher.backup.BackupService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileMoveEvent;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileMovedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

/**
 * Makes moving and renaming profiles safe with regard to their files and backups:
 * <ul>
 *     <li>A profile whose game directory is in use by a running game is not moved,
 *     see {@link ProfileUsageService}.</li>
 *     <li>Backups are stored under the name of their profile. When a profile is renamed its backups are
 *     renamed with it, otherwise they would not be found for the profile anymore.</li>
 * </ul>
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ProfileMoveObserver {
    private final ProfileUsageService usageService;
    private final ProfileBackupProvider provider;
    private final BackupService backupService;

    /**
     * Prevents moving the game directory of a profile that is in use.
     * Renaming a profile does not touch its files, so that is always possible.
     */
    void beforeMove(@Observes ProfileMoveEvent event) {
        Profile profile = event.profile();
        if (!event.isMove() || !Files.isDirectory(profile.path())) {
            return;
        }

        // the whole game directory
        BackupScope scope = new BackupScope(profile.path(), List.of(""), List.of());
        Optional<String> usage = usageService.getUsage(scope, profile);
        if (usage.isPresent()) {
            throw new IllegalStateException(usage.get() + ", stop it before moving profile " + profile.name() + ".");
        }
    }

    void afterMove(@Observes ProfileMovedEvent event) {
        if (!event.isRename()) {
            return;
        }

        List<Backup> backups = backupService.rename(provider.getName(), event.before().name(), event.after().name());
        if (!backups.isEmpty()) {
            log.info("Moved {} backups of profile {} to {}", backups.size(), event.before().name(), event.after().name());
        }
    }

}

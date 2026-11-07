package io.github.headlesshq.headlessmc.commands.backup;

import io.github.headlesshq.headlessmc.launcher.backup.Backup;
import io.github.headlesshq.headlessmc.launcher.backup.BackupService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;

import java.util.Iterator;

/**
 * Completes the targets that have backups.
 */
@Default
@Dependent
@RegisterForReflection
@Named("headlessmc:picocli:completions:backup-targets") // prevent Bean from getting removed by Quarkus during build
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class BackupTargetCompletions implements Iterable<String> {
    private final BackupService backupService;

    @Override
    public Iterator<String> iterator() {
        return backupService.list().stream()
            .map(Backup::target)
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .iterator();
    }

}

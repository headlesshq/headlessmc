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

@Default
@Dependent
@RegisterForReflection
@Named("headlessmc:picocli:completions:backups") // prevent Bean from getting removed by Quarkus during build
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class BackupCompletions implements Iterable<String> {
    private final BackupService backupService;

    @Override
    public Iterator<String> iterator() {
        return backupService.list().stream()
            .map(Backup::id)
            .iterator();
    }

}

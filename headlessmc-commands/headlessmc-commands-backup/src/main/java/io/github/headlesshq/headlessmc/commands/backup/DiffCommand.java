package io.github.headlesshq.headlessmc.commands.backup;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.launcher.backup.Backup;
import io.github.headlesshq.headlessmc.launcher.backup.BackupDiff;
import io.github.headlesshq.headlessmc.launcher.backup.BackupService;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
@Setter
@Default
@Dependent
@Named("command:backup:diff")
@CommandLine.Command(
    name = "diff",
    mixinStandardHelpOptions = true,
    description = "Shows the difference between a backup and the files on disk."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DiffCommand implements Runnable, CachedConsole.Enabled {
    private final BackupService backupService;
    private final TableProvider tableProvider;
    private final Console console;

    @CommandLine.Parameters(
        paramLabel = "backup",
        description = "The id of the backup, or a profile/server to compare its latest backup.",
        completionCandidates = BackupCompletions.class
    )
    private @Nullable String backup;

    @CommandLine.Option(
        names = {"-c", "--checksum"},
        description = "Compares the contents of files whose size and modification time did not change."
    )
    private boolean checksum;

    @Override
    public void run() {
        if (backup == null) {
            throw new IllegalArgumentException("Please specify the backup to compare.");
        }

        Backup resolved = backupService.get(backup)
            .or(() -> backupService.list(null, backup).stream().max(Comparator.comparing(Backup::created)))
            .orElseThrow(() -> new IllegalArgumentException("Failed to find backup " + backup));

        BackupDiff diff = backupService.diff(resolved, checksum);
        if (diff.isEmpty()) {
            console.write("No differences between backup " + resolved.id() + " and " + resolved.source());
            return;
        }

        List<Change> changes = new ArrayList<>();
        diff.added().forEach(file -> changes.add(new Change("added", file)));
        diff.removed().forEach(file -> changes.add(new Change("removed", file)));
        diff.modified().forEach(file -> changes.add(new Change("modified", file)));
        changes.sort(Comparator.comparing(Change::file));

        tableProvider.<Change>get()
            .withColumn("change", Change::type)
            .withColumn("file", Change::file)
            .addAll(changes)
            .log(console::write);
        console.write(
            diff.added().size() + " added, " + diff.removed().size() + " removed, "
                + diff.modified().size() + " modified since backup " + resolved.id()
        );
    }

    private record Change(String type, String file) {

    }

}

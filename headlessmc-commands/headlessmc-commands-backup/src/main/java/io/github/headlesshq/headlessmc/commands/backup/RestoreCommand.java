package io.github.headlesshq.headlessmc.commands.backup;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.launcher.backup.*;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

@Getter
@Setter
@Default
@Dependent
@Named("command:backup:restore")
@CommandLine.Command(
    name = "restore",
    mixinStandardHelpOptions = true,
    description = "Restores a backup, files added since the backup are deleted. "
        + "Backs up the files that are about to be changed first."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class RestoreCommand implements Runnable, CachedConsole.Enabled {
    private final BackupService backupService;
    private final Console console;

    @CommandLine.Parameters(
        paramLabel = "backup",
        description = "The id of the backup to restore.",
        completionCandidates = BackupCompletions.class
    )
    private @Nullable String id;

    @CommandLine.Option(
        names = {"--no-backup"},
        description = "Does not back up the files on disk before restoring."
    )
    private boolean noBackup;

    @CommandLine.Option(
        names = {"-k", "--keep-new"},
        description = "Keeps files that were added since the backup, instead of deleting them."
    )
    private boolean keepNew;

    @CommandLine.Option(
        names = {"-f", "--force"},
        description = "Restores even if the game is running."
    )
    private boolean force;

    @Override
    public void run() {
        if (id == null) {
            throw new IllegalArgumentException("Please specify the id of the backup to restore.");
        }

        Backup backup = backupService.get(id)
            .orElseThrow(() -> new IllegalArgumentException("Failed to find backup " + id));

        RestoreResult result = backupService.restore(backup, new RestoreOptions(!noBackup, keepNew, force));
        BackupDiff changes = result.changes();
        if (changes.isEmpty()) {
            console.write(backup.source() + " already matches backup " + backup.id());
            return;
        }

        Backup safetyBackup = result.safetyBackup();
        if (safetyBackup != null) {
            console.write("Backed up the current files as " + safetyBackup.id());
        }

        console.write(
            "Restored backup " + backup.id() + " to " + backup.source() + ": deleted " + changes.added().size()
                + ", restored " + changes.removed().size() + " and reverted " + changes.modified().size() + " files"
        );
    }

}

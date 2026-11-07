package io.github.headlesshq.headlessmc.commands.backup;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.launcher.backup.Backup;
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

@Getter
@Setter
@Default
@Dependent
@Named("command:backup:remove")
@CommandLine.Command(
    name = "remove",
    aliases = {"rm"},
    mixinStandardHelpOptions = true,
    description = "Deletes a backup."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class RemoveCommand implements Runnable, CachedConsole.Enabled {
    private final BackupService backupService;
    private final Console console;

    @CommandLine.Parameters(
        paramLabel = "backup",
        description = "The id of the backup to delete.",
        completionCandidates = BackupCompletions.class
    )
    private @Nullable String id;

    @Override
    public void run() {
        if (id == null) {
            throw new IllegalArgumentException("Please specify the id of the backup to delete.");
        }

        Backup backup = backupService.get(id)
            .orElseThrow(() -> new IllegalArgumentException("Failed to find backup " + id));

        backupService.remove(backup);
        console.write("Removed backup " + backup.id());
    }

}

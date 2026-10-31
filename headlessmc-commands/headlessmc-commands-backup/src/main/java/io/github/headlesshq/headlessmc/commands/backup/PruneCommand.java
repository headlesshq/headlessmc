package io.github.headlesshq.headlessmc.commands.backup;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.launcher.backup.Backup;
import io.github.headlesshq.headlessmc.launcher.backup.BackupOrigin;
import io.github.headlesshq.headlessmc.launcher.backup.BackupService;
import io.github.headlesshq.headlessmc.launcher.backup.PruneRequest;
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Default
@Dependent
@Named("command:backup:prune")
@CommandLine.Command(
    name = "prune",
    mixinStandardHelpOptions = true,
    description = "Deletes old backups. Keeps the newest --keep backups of each profile/server "
        + "and only deletes backups older than --older-than."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class PruneCommand implements Runnable, CachedConsole.Enabled {
    private final BackupService backupService;
    private final BackupFormatter formatter;
    private final Console console;

    @CommandLine.Parameters(
        paramLabel = "profile/server",
        description = "Only prunes the backups of this profile/server.",
        arity = "0..1",
        completionCandidates = BackupTargetCompletions.class
    )
    private @Nullable String target;

    @CommandLine.Option(
        names = {"-p", "--provider"},
        description = "Only prunes the backups of this provider.",
        completionCandidates = BackupProviderCompletions.class
    )
    private @Nullable String provider;

    @CommandLine.Option(
        names = {"-k", "--keep"},
        description = "How many of the newest backups of each profile/server to keep."
    )
    private @Nullable Integer keep;

    @CommandLine.Option(
        names = {"--older-than"},
        description = "Only deletes backups older than this, e.g. 30d, 12h or 2w."
    )
    private @Nullable String olderThan;

    @CommandLine.Option(
        names = {"-o", "--origin"},
        description = "Only prunes backups created by: 'manual', 'launch' or 'restore'.",
        split = ",",
        completionCandidates = BackupOriginCompletions.class
    )
    private List<String> origins = new ArrayList<>();

    @Override
    public void run() {
        Set<BackupOrigin> parsedOrigins = origins.stream().map(BackupOrigin::parse).collect(Collectors.toSet());
        List<Backup> removed = backupService.prune(new PruneRequest(
            provider, target, parsedOrigins, keep, olderThan == null ? null : formatter.duration(olderThan)
        ));

        removed.forEach(backup -> console.write("Removed backup " + backup.id()));
        console.write("Removed " + removed.size() + " backups.");
    }

}

package io.github.headlesshq.headlessmc.commands.backup;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.format.TableBuilder;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
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

import java.util.List;

@Getter
@Setter
@Default
@Dependent
@Named("command:backup:list")
@CommandLine.Command(
    name = "list",
    aliases = {"ls"},
    mixinStandardHelpOptions = true,
    description = "Lists backups."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ListCommand implements Runnable, CachedConsole.Enabled {
    private final BackupService backupService;
    private final BackupFormatter formatter;
    private final TableProvider tableProvider;
    private final Console console;

    @CommandLine.Option(
        names = {"-p", "--provider"},
        description = "Only lists the backups of this provider.",
        completionCandidates = BackupProviderCompletions.class
    )
    private @Nullable String provider;

    @CommandLine.Parameters(
        paramLabel = "profile/server",
        description = "Only lists the backups of this profile/server.",
        arity = "0..1",
        completionCandidates = BackupTargetCompletions.class
    )
    private @Nullable String target;

    @Override
    public void run() {
        List<Backup> backups = backupService.list(provider, target);
        TableBuilder<Backup> table = tableProvider.<Backup>get()
            .withColumn("id", Backup::id);
        if (backups.stream().map(Backup::provider).distinct().count() > 1) {
            table = table.withColumn("provider", Backup::provider);
        }

        table.withColumn("target", Backup::target)
            .withColumn("type", Backup::type)
            .withColumn("origin", backup -> backup.origin().getName())
            .withColumn("contents", formatter::contents)
            .withColumn("created", formatter::created)
            .withColumn("files", backup -> String.valueOf(backup.files()))
            .withColumn("size", backup -> formatter.size(backup.size()))
            .addAll(backups)
            .log(console::write);
    }

}

package io.github.headlesshq.headlessmc.commands.backup;

import io.github.headlesshq.headlessmc.commands.util.WorldCompletions;
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

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Default
@Dependent
@Named("command:backup")
@CommandLine.Command(
    name = "backup",
    mixinStandardHelpOptions = true,
    description = "Backs up a profile/server or its worlds.",
    subcommands = {
        ListCommand.class,
        DiffCommand.class,
        RestoreCommand.class,
        RemoveCommand.class,
        PruneCommand.class
    }
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class BackupCommand implements Runnable, CachedConsole.Enabled {
    private final BackupService backupService;
    private final BackupFormatter formatter;
    private final Console console;

    @CommandLine.Option(
        names = {"-p", "--provider"},
        description = "What to back up, defaults to 'profile' for profiles and servers.",
        completionCandidates = BackupProviderCompletions.class
    )
    private String provider = BackupProviderService.DEFAULT_PROVIDER;

    @CommandLine.Option(
        names = {"-t", "--type"},
        description = "What to back up of a profile: 'full' (the game directory and the profile), 'worlds' or 'world'. "
            + "Defaults to 'world' if --world is given and 'full' otherwise.",
        completionCandidates = BackupTypeCompletions.class
    )
    private @Nullable String type;

    @CommandLine.Option(
        names = {"-w", "--world"},
        description = "The world to back up with --type world.",
        completionCandidates = WorldCompletions.class
    )
    private @Nullable String world;

    @CommandLine.Option(
        names = {"-i", "--ignore"},
        description = "Glob patterns of paths to leave out of the backup, relative to the game directory, "
            + "in addition to hmc.backup.ignore, e.g. --ignore screenshots,**/*.log",
        split = ","
    )
    private List<String> ignore = new ArrayList<>();

    @CommandLine.Option(
        names = {"--no-default-ignore"},
        description = "Does not leave out the paths of hmc.backup.ignore and, for full backups of clients, "
            + "the assets, libraries and versions directories."
    )
    private boolean noDefaultIgnore;

    @CommandLine.Option(
        names = {"-f", "--force"},
        description = "Backs up even if the game is running."
    )
    private boolean force;

    @CommandLine.Parameters(
        paramLabel = "profile/server",
        description = "What to back up, e.g. the name of a profile/server.",
        arity = "0..*",
        completionCandidates = BackupSourceCompletions.class
    )
    private List<String> target = new ArrayList<>();

    @Override
    public void run() {
        if (target.isEmpty()) {
            throw new IllegalArgumentException("Please specify what to back up, e.g. the name of a profile/server.");
        }

        Backup backup = backupService.create(new BackupRequest(
            provider, String.join(" ", target), null, type, world, ignore,
            !noDefaultIgnore, BackupOrigin.MANUAL, force
        ));

        console.write(
            "Created " + backup.type() + " backup " + backup.id() + " of " + backup.target()
                + " (" + backup.files() + " files, " + formatter.size(backup.size()) + ") at "
                + backupService.getArchive(backup)
        );
    }

}

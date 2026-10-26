package io.github.headlesshq.headlessmc.commands.version;

import io.github.headlesshq.headlessmc.commands.util.VersionArgCommand;
import io.github.headlesshq.headlessmc.commands.util.VersionCompletions;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.launcher.client.VersionRemoval;
import io.github.headlesshq.headlessmc.launcher.client.VersionRemoveService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.version.Version;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Getter
@Setter
@Default
@Dependent
@Named("command:version:remove")
@CommandLine.Command(
    name = "remove",
    aliases = {"delete", "rm", "uninstall"},
    mixinStandardHelpOptions = true,
    description = "Deletes an installed version and the libraries and assets no other version needs."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class RemoveCommand implements Runnable, VersionArgCommand, CachedConsole.Enabled {
    private final VersionRemoveService versionRemoveService;
    private final Console console;

    @CommandLine.Option(
        names = {"-f", "--force"},
        description = "Deletes the version even if profiles or other versions need it.",
        defaultValue = "false"
    )
    private boolean force;

    @CommandLine.Option(
        names = {"--dry-run"},
        description = "Only lists the files that would be deleted.",
        defaultValue = "false"
    )
    private boolean dryRun;

    @CommandLine.Option(
        names = {"-a", "--all"},
        description = "Also deletes the versions inheriting from the version and the profiles that need it, requires --force.",
        defaultValue = "false"
    )
    private boolean all;

    @CommandLine.Parameters(
        paramLabel = "VERSION",
        description = "The version to delete, e.g. 'fabric 1.21.1 0.16.9', or the name of its version.json.",
        completionCandidates = VersionCompletions.class
    )
    private List<String> versionArg = new ArrayList<>();

    @Override
    public void run() {
        if (all && !force) {
            throw new IllegalArgumentException("--all deletes other versions and profiles, it can only be used together with --force.");
        }

        Version version = versionRemoveService.findVersion(versionArg);
        VersionRemoval removal = versionRemoveService.plan(version, all);
        if (removal.isBlocked() && !force) {
            throw new IllegalArgumentException(blockedMessage(removal));
        }

        if (dryRun) {
            console.write("Would delete " + summary(removal) + ":");
            removal.files().stream().map(Path::toString).forEach(console::write);
            return;
        }

        if (all && !confirm(removal)) {
            console.write("Aborted, nothing was deleted.");
            return;
        }

        versionRemoveService.remove(removal);
        console.write("Deleted " + summary(removal));
    }

    private boolean confirm(VersionRemoval removal) {
        String answer = console.read("This will delete " + summary(removal) + ". Continue? [y/N] ");
        return answer != null && List.of("y", "yes").contains(answer.trim().toLowerCase(Locale.ENGLISH));
    }

    private static String summary(VersionRemoval removal) {
        StringBuilder summary = new StringBuilder("versions ")
            .append(removal.removedVersions().stream().map(Version::getId).toList());
        if (!removal.removedProfiles().isEmpty()) {
            summary.append(" and profiles ").append(removal.removedProfiles().stream().map(Profile::name).toList());
        }

        return summary.append(" (").append(removal.files().size()).append(" files)").toString();
    }

    private String blockedMessage(VersionRemoval removal) {
        StringBuilder message = new StringBuilder("Cannot delete version ").append(removal.version().getId());
        if (!removal.dependentVersions().isEmpty()) {
            message.append(", versions inheriting from it: ")
                .append(removal.dependentVersions().stream().map(Version::getId).toList());
        }

        if (!removal.blockingProfiles().isEmpty()) {
            message.append(", profiles that need it: ")
                .append(removal.blockingProfiles().stream().map(Profile::name).toList());
        }

        return message.append(". Delete them first, use --force to delete it anyway, or --force --all to delete them too.").toString();
    }

}

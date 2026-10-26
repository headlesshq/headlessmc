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

@Getter
@Setter
@Default
@Dependent
@Named("command:version:delete")
@CommandLine.Command(
    name = "delete",
    aliases = {"remove", "rm", "uninstall"},
    mixinStandardHelpOptions = true,
    description = "Deletes an installed version and the libraries and assets no other version needs."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DeleteCommand implements Runnable, VersionArgCommand, CachedConsole.Enabled {
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

    @CommandLine.Parameters(
        paramLabel = "VERSION",
        description = "The version to delete, e.g. 'fabric 1.21.1 0.16.9', or the name of its version.json.",
        completionCandidates = VersionCompletions.class
    )
    private List<String> versionArg = new ArrayList<>();

    @Override
    public void run() {
        Version version = versionRemoveService.findVersion(versionArg);
        VersionRemoval removal = versionRemoveService.plan(version);
        if (removal.isBlocked() && !force) {
            throw new IllegalArgumentException(blockedMessage(removal));
        }

        if (dryRun) {
            console.write("Deleting version " + version.getId() + " would delete " + removal.files().size() + " files:");
            removal.files().stream().map(Path::toString).forEach(console::write);
            return;
        }

        versionRemoveService.remove(removal);
        console.write("Deleted version " + version.getId() + " (" + removal.files().size() + " files)");
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

        return message.append(". Delete them first or use --force.").toString();
    }

}

package io.github.headlesshq.headlessmc.commands.mod;

import io.github.headlesshq.headlessmc.commands.util.ProfileVersionCompletions;
import io.github.headlesshq.headlessmc.commands.util.VersionArgCommand;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.format.TableBuilder;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
@Setter
@Default
@Dependent
@Named("command:mod:remove")
@CommandLine.Command(
    name = "remove",
    aliases = {"rm"},
    mixinStandardHelpOptions = true,
    description = "Removes an installed mod from a profile."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class RemoveCommand implements Runnable, VersionArgCommand, CachedConsole.Enabled {
    private final ModListingService modListingService;
    private final ProfileService profileService;
    private final TableProvider tableProvider;
    private final Console console;

    @CommandLine.Option(
        names = {"-t", "--type"},
        description = "The type of the mod, e.g. 'mod', 'resourcepack', 'modpack', 'datapack', 'shader'.",
        completionCandidates = ModTypeCompletions.class
    )
    private @Nullable String type;

    @CommandLine.Option(
        names = {"-w", "--world"},
        description = "The world to look for datapacks in.",
        completionCandidates = WorldCompletions.class
    )
    private @Nullable String world;

    @CommandLine.Option(
        names = {"--platform"},
        description = "Override the profiles platform.",
        completionCandidates = PlatformCompletions.class
    )
    private @Nullable String platform;

    @CommandLine.Option(
        names = "--file",
        description = "The name of the file to delete."
    )
    private @Nullable String file;

    @CommandLine.Option(
        names = {"-f", "--force"},
        description = "Delete mods despite conflicts."
    )
    private boolean force;

    @CommandLine.Option(
        names = {"--all"},
        description = "Deletes mods from multiple profiles."
    )
    private boolean all;

    @CommandLine.Parameters(
        description = "The id of the mod to remove.",
        paramLabel = "mod",
        index = "0",
        arity = "1"
    )
    private @Nullable String mod;

    @CommandLine.Parameters(
        paramLabel = "profile",
        index = "1",
        arity = "0..*",
        description = "The profile to remove the mod from.",
        completionCandidates = ProfileVersionCompletions.class
    )
    private List<String> versionArg = new ArrayList<>();

    @Override
    public void run() {
        String mod = this.mod;
        String file = this.file;
        if (mod == null && file == null) {
            throw new IllegalArgumentException("Please specify the id or file name of the mod to remove.");
        }

        if (versionArg.isEmpty()) {
            deleteFromAllProfiles(mod, file);
            return;
        }

        List<ModFile> files = filter(modListingService.list(versionArg, platform, world, type), mod, file);
        if (files.isEmpty()) {
            //noinspection DataFlowIssue
            throwOnFilesEmpty(mod, file);
            return;
        }

        if (files.size() > 1) {
            if (force) {
                for (ModFile modFile : files) {
                    try {
                        Files.delete(modFile.file());
                    } catch (IOException e) {
                        throw new HeadlessMcIOException("Failed to delete mod " + modFile.name(), e);
                    }
                }

                return;
            }

            throw new HeadlessMcException(
                "Multiple files matched. Please narrow down the mod to delete via " +
                    "--file, --type, --world, or --platform or use -f/--force\n"
                + table(files)
            );
        }

        try {
            Files.delete(files.getFirst().file());
            console.write("Deleted mod successfully.");
        } catch (IOException e) {
            throw new HeadlessMcIOException("Failed to delete mod " + files.getFirst().name(), e);
        }
    }

    private String table(List<ModFile> files) {
        return tableBuilder()
            .addAll(files)
            .toString();
    }

    private TableBuilder<ModFile> tableBuilder() {
        return tableProvider.<ModFile>get()
            .withColumn("id", ModFile::id)
            .withColumn("name", ModFile::name)
            .withColumn("authors", file -> String.join(", ", file.authors()))
            .withColumn("file", file -> file.file().getFileName().toString())
            .withColumn("type", file -> file.type().name());
    }

    private List<ModFile> filter(List<ModFile> files, @Nullable String mod, @Nullable String file) {
        return files.stream()
            .filter(modFile -> mod == null || modFile.id().equalsIgnoreCase(mod))
            .filter(modFile -> file == null || modFile.file().toString().equals(file) || modFile.file().getFileName().toString().equals(file))
            .toList();
    }

    private void throwOnFilesEmpty(@Nullable String mod, @Nullable String file) {
        if (mod == null) {
            throw new IllegalArgumentException("Failed to find mod file " + file);
        } else if (file == null) {
            throw new IllegalArgumentException("Failed to find mod " + mod);
        } else {
            throw new IllegalArgumentException("Failed to find mod " + mod + " with file " + file);
        }
    }

    private void deleteFromAllProfiles(@Nullable String mod, @Nullable String file) {
        Map<Profile, List<ModFile>> files = new HashMap<>();
        for (Profile profile : profileService.getProfiles()) {
            List<ModFile> modFiles = filter(modListingService.list(profile, platform, world, type), mod, file);
            if (modFiles.size() > 1 && !force) {
                throw new HeadlessMcException(
                    "Multiple files matched for profile " + profile.name()
                        + ". Please narrow down the mod to delete via " +
                        "--profile, --file, --type, --world, or --platform or use -f/--force\n"
                        + table(modFiles)
                );
            }

            if (!modFiles.isEmpty()) {
                files.put(profile, modFiles);
            }
        }

        if (files.isEmpty()) {
            //noinspection DataFlowIssue
            throwOnFilesEmpty(mod, file);
            return;
        }

        if (all) {
            files.forEach((_, modFiles) -> {
                for (ModFile modFile : modFiles) {
                    try {
                        Files.delete(modFile.file());
                    } catch (IOException e) {
                        throw new HeadlessMcIOException("Failed to delete mod " + modFile.name(), e);
                    }
                }
            });

            console.write("Deleted mod successfully.");
        } else {
            console.write("Found mod files across " + files.size() + " profiles.\nSpecify --all or --profile to delete them:");
            tableBuilder()
                .withColumn("profile", modFile -> files.entrySet()
                    .stream()
                    .filter(e -> e.getValue().stream().anyMatch(f -> f == modFile))
                    .findFirst()
                    .map(e -> e.getKey().name())
                    .orElse("-")
                )
                .addAll(files.values().stream().flatMap(List::stream).toList())
                .log(console::write);
        }
    }

}

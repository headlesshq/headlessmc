package io.github.headlesshq.headlessmc.commands.java;

import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaService;
import io.github.headlesshq.headlessmc.java.JavaSource;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.nio.file.Path;

@Getter
@Setter
@Default
@Dependent
@Named("command:java:remove")
@CommandLine.Command(
    name = "remove",
    aliases = "rm",
    mixinStandardHelpOptions = true,
    description = "Removes an installed java version."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class RemoveCommand implements Runnable, CachedConsole.Enabled {
    private final JavaService javaService;
    private final FileService fileService;
    private final Console console;

    @CommandLine.Parameters(
        description = "The name of the java installation to remove.",
        completionCandidates = InstalledJavaCompletions.class
    )
    private @Nullable String name;

    @Override
    public void run() {
        if (name == null) {
            throw new IllegalArgumentException("Please specify the name of the java installation to remove.");
        }

        Java java = javaService.getJavaVersions().stream()
            .filter(candidate -> name.equalsIgnoreCase(candidate.name()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Failed to find java installation " + name));

        if (java.source() != JavaSource.SORT_HMC) {
            console.write(
                "Warning: " + java.name() + " (" + java.home() + ") was not installed by HeadlessMc."
            );
        }

        Path path = java.home().getUnchecked()
            .orElseThrow(() -> new IllegalArgumentException("Failed to resolve path of java installation " + name));

        fileService.delete(path);
        javaService.refresh();
        console.write("Removed java installation " + name);
    }

}

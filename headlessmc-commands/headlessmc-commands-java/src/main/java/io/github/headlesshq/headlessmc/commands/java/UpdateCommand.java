package io.github.headlesshq.headlessmc.commands.java;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.distribution.JavaRuntime;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Objects;

@Slf4j
@Getter
@Setter
@Default
@Dependent
@Named("command:java:update")
@CommandLine.Command(
    name = "update",
    aliases = "upgrade",
    mixinStandardHelpOptions = true,
    description = "Updates the java installations installed by HeadlessMc to the newest version of their distribution."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class UpdateCommand implements Runnable, CachedConsole.Enabled {
    private final JavaUpdateService updateService;
    private final TableProvider tableProvider;
    private final Console console;

    @CommandLine.Option(
        names = {"--dry-run"},
        description = "Only lists the java installations that can be updated.",
        defaultValue = "false"
    )
    private boolean dryRun;

    @Override
    public void run() {
        JavaUpdateService.Result result = updateService.check();
        for (JavaUpdateService.Skipped skipped : result.skipped()) {
            console.write("Skipped " + skipped.java().name() + ": " + skipped.reason());
        }

        if (result.updates().isEmpty()) {
            console.write("All java installations are up-to-date.");
            return;
        }

        tableProvider.<JavaUpdateService.Update>get()
            .withColumn("name", update -> update.java().name())
            .withColumn("distribution", update -> update.installed().distribution())
            .withColumn("installed", update -> versionOf(update.installed()))
            .withColumn("latest", update -> versionOf(update.latest()))
            .addAll(result.updates())
            .log(console::write);

        if (dryRun) {
            console.write(result.updates().size() + " java installation(s) can be updated.");
            return;
        }

        int failed = 0;
        for (JavaUpdateService.Update update : result.updates()) {
            try {
                Path path = updateService.update(update);
                console.write("Updated " + update.java().name() + " to " + versionOf(update.latest()) + " at " + path);
            } catch (HeadlessMcException e) {
                log.error("Failed to update {}", update.java().name(), e);
                console.write("Failed to update " + update.java().name() + ": " + e.getMessage());
                failed++;
            }
        }

        if (failed > 0) {
            throw new IllegalStateException("Failed to update " + failed + " java installation(s).");
        }
    }

    private static String versionOf(JavaRuntime runtime) {
        return Objects.requireNonNullElse(runtime.javaVersion(), String.valueOf(runtime.version()));
    }

}

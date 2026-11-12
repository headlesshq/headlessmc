package io.github.headlesshq.headlessmc.application;

import io.github.headlesshq.headlessmc.config.I18nService;
import io.github.headlesshq.headlessmc.plugins.HeadlessMcSubcommand;
import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;

/**
 * Produces the {@link CommandLine} quarkus-picocli would otherwise produce,
 * with an {@link CommandLine.IExecutionExceptionHandler} which logs
 * exceptions instead of letting picocli print their stacktrace.
 * Beans implementing {@link HeadlessMcSubcommand}, e.g. from plugins, are added as subcommands.
 * <p>
 * Picocli prints unhandled exceptions with
 * {@link CommandLine#getErr()} itself, which bypasses the logger and
 * therefore also the {@code hmc.log.*} configuration.
 */
@Slf4j
@Dependent
public class CommandLineProducer {
    /**
     * Produces the {@link CommandLine} for the application and the shell.
     * Overrides the default Bean of quarkus-picocli.
     *
     * @param factory     the factory quarkus-picocli uses itself.
     * @param i18nService resolves the resource bundle holding localized descriptions.
     * @param subcommands additional subcommands, e.g. from plugins.
     * @return a new {@link CommandLine}.
     */
    @Produces
    @Dependent
    public CommandLine commandLine(
        PicocliCommandLineFactory factory,
        I18nService i18nService,
        @Any Instance<HeadlessMcSubcommand> subcommands
    ) {
        CommandLine commandLine = factory.create();
        for (HeadlessMcSubcommand subcommand : subcommands) {
            commandLine.addSubcommand(subcommand);
        }

        return commandLine
            .setExecutionExceptionHandler(CommandLineProducer::handleExecutionException)
            // null if no translation is installed, picocli then uses the annotations.
            .setResourceBundle(i18nService.bundle());
    }

    private static int handleExecutionException(
        Exception exception,
        CommandLine commandLine,
        @Nullable ParseResult parseResult
    ) {
        log.info("Failed to execute {}", commandLine.getCommandSpec().qualifiedName(), exception);
        // TODO: should probably use the console to log this?
        log.error("", exception);
        CommandLine.IExitCodeExceptionMapper mapper = commandLine.getExitCodeExceptionMapper();
        return mapper == null
            ? commandLine.getCommandSpec().exitCodeOnExecutionException()
            : mapper.getExitCode(exception);
    }

}

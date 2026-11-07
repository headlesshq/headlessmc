package io.github.headlesshq.headlessmc.application;

import io.github.headlesshq.headlessmc.config.I18nService;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.ConsoleException;
import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;

/**
 * Produces the {@link CommandLine} quarkus-picocli would otherwise produce,
 * with an {@link CommandLine.IExecutionExceptionHandler} which logs
 * exceptions instead of letting picocli print their stacktrace.
 * <p>
 * Picocli prints unhandled exceptions with
 * {@link CommandLine#getErr()} itself, which bypasses the logger and
 * therefore also the {@code hmc.log.*} configuration.
 * <p>
 * Commands aborted by the user, e.g. with Ctrl+C while being prompted
 * (a {@link ConsoleException.Interrupted}), are not errors:
 * they are reported with a single line on the {@link Console} and exit with
 * {@link ConsoleException.Interrupted#EXIT_CODE}.
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
     * @param console     the console to report aborted commands on.
     * @return a new {@link CommandLine}.
     */
    @Produces
    @Dependent
    public CommandLine commandLine(PicocliCommandLineFactory factory, I18nService i18nService, Console console) {
        return factory.create()
            .setExecutionExceptionHandler((exception, commandLine, parseResult) ->
                handleExecutionException(console, exception, commandLine, parseResult))
            // null if no translation is installed, picocli then uses the annotations.
            .setResourceBundle(i18nService.bundle());
    }

    static int handleExecutionException(
        Console console,
        Exception exception,
        CommandLine commandLine,
        @Nullable ParseResult parseResult
    ) {
        ConsoleException.Interrupted interrupted = ConsoleException.Interrupted.find(exception);
        if (interrupted != null) {
            log.debug("Aborted {}", commandLine.getCommandSpec().qualifiedName(), exception);
            console.write(interrupted.getMessage() == null || interrupted.getMessage().isEmpty()
                ? "Aborted."
                : "Aborted: " + interrupted.getMessage());
            return ConsoleException.Interrupted.EXIT_CODE;
        }

        log.info("Failed to execute {}", commandLine.getCommandSpec().qualifiedName(), exception);
        // TODO: should probably use the console to log this?
        log.error("", exception);
        CommandLine.IExitCodeExceptionMapper mapper = commandLine.getExitCodeExceptionMapper();
        return mapper == null
            ? commandLine.getCommandSpec().exitCodeOnExecutionException()
            : mapper.getExitCode(exception);
    }

}

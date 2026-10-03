package io.github.headlesshq.headlessmc.application;

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
 */
@Slf4j
@Dependent
public class CommandLineProducer {
    /**
     * Produces the {@link CommandLine} for the application and the shell.
     * Overrides the default Bean of quarkus-picocli.
     *
     * @param factory the factory quarkus-picocli uses itself.
     * @return a new {@link CommandLine}.
     */
    @Produces
    @Dependent
    public CommandLine commandLine(PicocliCommandLineFactory factory) {
        return factory.create().setExecutionExceptionHandler(CommandLineProducer::handleExecutionException);
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

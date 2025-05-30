package io.github.headlesshq.headlessmc.api.command.picocli;

/**
 * Picocli has an Exception handling system where no Exceptions make it to the outside.
 * However, we would like to get the exception and throw it.
 * Since Picocli does not catch {@link Error}s, we wrap our exceptions in the
 * {@link picocli.CommandLine.IExitCodeExceptionMapper}.
 */
public class CommandError extends Error {
    public CommandError(Throwable cause) {
        super(cause instanceof CommandError ? cause.getCause() : cause);
    }

}

package io.github.headlesshq.headlessmc.api.command;

import lombok.experimental.StandardException;

/**
 * A CommandException is meant to be used as a quick and easy way to exit the
 * execution of a command. It should only be used to communicate to the user
 * that something with his input went wrong.
 */
@StandardException
public class CommandException extends RuntimeException {
    /**
     * Constructs a new CommandException with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public CommandException(String message) {
        super(message);
    }

}

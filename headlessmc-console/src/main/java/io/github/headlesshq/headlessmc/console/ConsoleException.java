package io.github.headlesshq.headlessmc.console;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import lombok.experimental.StandardException;

/**
 * Thrown e.g. if a {@link ConsoleProvider} cannot provide a {@link Console},
 * or if there are other issues with a console.
 */
@StandardException
public class ConsoleException extends HeadlessMcException {
    @StandardException
    public static class Interrupted extends ConsoleException {

    }

}

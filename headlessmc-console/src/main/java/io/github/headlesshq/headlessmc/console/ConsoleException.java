package io.github.headlesshq.headlessmc.console;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import lombok.experimental.StandardException;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Thrown e.g. if a {@link ConsoleProvider} cannot provide a {@link Console},
 * or if there are other issues with a console.
 */
@StandardException
public class ConsoleException extends HeadlessMcException {
    /**
     * Thrown if the user aborted reading input, e.g. by pressing Ctrl+C or Ctrl+D,
     * if the input has been closed, or if the user declined to answer a {@link io.github.headlesshq.headlessmc.console.prompt.Form}.
     * This is not an error, commands failing with this exception are reported as aborted,
     * without a stacktrace.
     */
    @StandardException
    public static class Interrupted extends ConsoleException {
        /**
         * Exit code of commands that have been aborted, like a process terminated by SIGINT.
         */
        public static final int EXIT_CODE = 130;

        /**
         * @param throwable the throwable to check.
         * @return the {@link Interrupted} if it is the given throwable or one of its causes, otherwise {@code null}.
         */
        public static @Nullable Interrupted find(@Nullable Throwable throwable) {
            Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            while (throwable != null && seen.add(throwable)) {
                if (throwable instanceof Interrupted) {
                    return (Interrupted) throwable;
                }

                throwable = throwable.getCause();
            }

            return null;
        }
    }

}

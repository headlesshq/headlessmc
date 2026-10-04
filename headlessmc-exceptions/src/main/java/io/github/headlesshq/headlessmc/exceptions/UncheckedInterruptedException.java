package io.github.headlesshq.headlessmc.exceptions;

import lombok.experimental.StandardException;

/**
 * Wraps a {@link InterruptedException}.
 * Any method that can throw a {@link HeadlessMcException}
 * may also throw a {@link UncheckedInterruptedException}.
 * This method deliberately does not subclass {@link HeadlessMcException},
 * to force separate handling.
 *
 * @see InterruptedException
 */
@StandardException
public final class UncheckedInterruptedException extends RuntimeException {

}

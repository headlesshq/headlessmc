package io.github.headlesshq.headlessmc.exceptions;

import lombok.experimental.StandardException;

/**
 * Base class for all Exceptions thrown by HeadlessMc.
 * The only other Exception thrown by HeadlessMc
 * is the {@link UncheckedInterruptedException},
 * because we want to force separate handling
 * of interruptions.
 * Whenever a method may throw a {@link HeadlessMcException}
 * it could also throw a {@link UncheckedInterruptedException}.
 */
@StandardException
///*during development*/public class HeadlessMcException extends Exception {
/*at runtime*/public class HeadlessMcException extends RuntimeException {

}

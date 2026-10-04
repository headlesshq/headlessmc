package io.github.headlesshq.headlessmc.exceptions;

import lombok.experimental.StandardException;

import java.io.IOException;

/**
 * Wraps {@link IOException}s.
 *
 * @see IOException
 */
@StandardException
public class HeadlessMcIOException extends HeadlessMcException {

}

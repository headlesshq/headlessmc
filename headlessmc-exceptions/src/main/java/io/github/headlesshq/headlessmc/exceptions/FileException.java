package io.github.headlesshq.headlessmc.exceptions;

import lombok.experimental.StandardException;

/**
 * A type of {@link HeadlessMcIOException} that indicates
 * that something went wrong while writing, reading or
 * in another way handling Files.
 */
@StandardException
public class FileException extends HeadlessMcIOException {

}

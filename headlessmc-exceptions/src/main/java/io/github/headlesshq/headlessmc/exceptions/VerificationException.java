package io.github.headlesshq.headlessmc.exceptions;

import lombok.experimental.StandardException;

/**
 * Thrown when the hash and/or the size of a file/download etc. does not match
 * the expected hash and/or size.
 */
@StandardException
public class VerificationException extends HeadlessMcException {

}

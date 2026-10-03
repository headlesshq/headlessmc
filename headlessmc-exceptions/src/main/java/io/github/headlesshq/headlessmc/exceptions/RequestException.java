package io.github.headlesshq.headlessmc.exceptions;

import lombok.experimental.StandardException;

/**
 * Exception thrown when an API request fails.
 * E.g. a request to Mojang's, PaperMC's or Forge's API.
 */
@StandardException
public class RequestException extends HeadlessMcIOException {

}

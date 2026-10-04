package io.github.headlesshq.headlessmc.platform.client;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import lombok.experimental.StandardException;

/**
 * If something goes wrong matching a version.
 *
 * @see VersionMatcher
 * @see VersionMatcherService
 */

@StandardException
public class VersionMatchException extends HeadlessMcException {

}

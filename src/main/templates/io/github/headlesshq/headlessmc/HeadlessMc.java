package io.github.headlesshq.headlessmc;

import lombok.experimental.UtilityClass;

/**
 * Contains some information about the current HeadlessMc version.
 */
@UtilityClass
public final class HeadlessMc {
    /**
     * Name of the HeadlessMc application.
     */
    public static final String NAME = "HeadlessMc";

    /**
     * Version of the HeadlessMc application.
     */
    public static final String VERSION = "${version}";

    /**
     * Default user agent for HTTP requests.
     */
    public static final String DEFAULT_USER_AGENT = NAME + "/" + VERSION;

}

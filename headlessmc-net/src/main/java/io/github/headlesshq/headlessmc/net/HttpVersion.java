package io.github.headlesshq.headlessmc.net;

import org.jetbrains.annotations.ApiStatus;

/**
 * Represents an HTTP version.
 */
public enum HttpVersion {
    /**
     * HTTP version 1.1
     */
    HTTP_1_1,
    /**
     * HTTP version 2
     */
    HTTP_2,
    /**
     * HTTP version 3 (Currently unsupported by the default java HTTP client implementation)
     * Support is underway in Java 26.
     */
    @ApiStatus.Experimental
    HTTP_3,

}

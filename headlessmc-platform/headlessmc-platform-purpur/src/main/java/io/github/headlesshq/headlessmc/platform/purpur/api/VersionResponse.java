package io.github.headlesshq.headlessmc.platform.purpur.api;

import java.util.List;

/**
 * Response object for the {@code /v2/{project}/{version}} api.
 * <pre>
 * {@code
 * {
 *   "project": "purpur",
 *   "version": "1.21.1",
 *   "builds": {
 *     "latest": "2329",
 *     "all": [
 *       "2285",
 *       "2286",
 *       "2287",
 *       "2288",
 *       ...
 *      ]
 *   }
 * }
 * </pre>
 */
public record VersionResponse(String version, Builds builds) {
    public record Builds(String latest, List<String> all) {}

}

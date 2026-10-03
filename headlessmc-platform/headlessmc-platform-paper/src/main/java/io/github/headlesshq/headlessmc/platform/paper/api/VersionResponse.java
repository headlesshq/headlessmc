package io.github.headlesshq.headlessmc.platform.paper.api;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Response object for the {@code /v3/projects/{project}/versions/{version}} api.
 * <pre>
 * {@code
 * {
 *   "version": {
 *     "id": "1.12.2",
 *     "support": {
 *       "status": "UNSUPPORTED"
 *     },
 *     "java": {
 *       "version": {
 *         "minimum": 8
 *       },
 *       "flags": {
 *         "recommended": [
 *           "-XX:+AlwaysPreTouch",
 *           ...
 *         ]
 *       }
 *     }
 *   },
 *   "builds": [
 *     1620,
 *     1619,
 *     ...
 *   ]
 * }
 * }
 * </pre>
 */
public record VersionResponse(Version version, Support support, List<Integer> builds) {
    public record Version(String id, Java java) {}

    public record Java(JavaVersion version, Map<String, List<String>> flags) {}

    public record JavaVersion(Integer minimum) {}

    public record Support(String status, @Nullable String end) {}

}

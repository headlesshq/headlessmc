package io.github.headlesshq.headlessmc.platform.purpur.api;

import java.util.List;

/**
 * Response object for the {@code /v2/{project}} api.
 * <pre>
 * {@code
 * {
 *   "project": "purpur",
 *   "metadata": {
 *     "current": "26.1.2"
 *   },
 *   "versions": [
 *     "1.14.1",
 *     "1.14.2",
 *     "1.14.3",
 *     ...
 * }
 * </pre>
 */
public record ProjectResponse(String project, List<String> versions) {

}

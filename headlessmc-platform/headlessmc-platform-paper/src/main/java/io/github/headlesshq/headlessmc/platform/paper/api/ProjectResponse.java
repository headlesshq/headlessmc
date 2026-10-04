package io.github.headlesshq.headlessmc.platform.paper.api;

import java.util.List;
import java.util.Map;

/**
 * Response object for the {@code /v3/projects/{project}} api.
 * Versions are grouped, e.g. the group {@code "26.1"} contains the versions {@code "26.1.2"} and {@code "26.1.1"},
 * but there is no version {@code "26.1"}.
 * <pre>
 * {@code
 * {
 *   "project": {
 *     "id": "paper",
 *     "name": "Paper"
 *   },
 *   "versions": {
 *     "26.1": ["26.1.2", "26.1.1"],
 *     "1.21": ["1.21.11", "1.21.11-rc3", ..., "1.21"],
 *     ...
 *   }
 * }
 * }
 * </pre>
 */
public record ProjectResponse(Project project, Map<String, List<String>> versions) {
    public record Project(String id, String name) {}

}

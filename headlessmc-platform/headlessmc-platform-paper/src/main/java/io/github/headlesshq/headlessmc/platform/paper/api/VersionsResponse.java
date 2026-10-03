package io.github.headlesshq.headlessmc.platform.paper.api;

import java.util.List;

/**
 * Response object for the {@code /v3/projects/{project}/versions} api.
 * <pre>
 * {@code
 * {
 *   "versions": [
 *     {
 *       "version": {
 *         "id": "1.21.5",
 *         ...
 *       },
 *       "builds": [
 *         114,
 *         ...
 *       ]
 *     },
 *     ...
 *   ]
 * }
 * }
 * </pre>
 *
 * @see VersionResponse
 */
public record VersionsResponse(List<VersionResponse> versions) {}

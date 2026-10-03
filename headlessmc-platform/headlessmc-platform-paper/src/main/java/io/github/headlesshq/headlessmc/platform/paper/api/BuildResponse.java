package io.github.headlesshq.headlessmc.platform.paper.api;

import java.util.Map;

/**
 * Response object for the {@code /v3/projects/{project}/versions/{version}/builds} api.
 * <pre>
 * {@code
 * {
 *   "id": 1620,
 *   "time": "2021-12-20T00:10:50.686Z",
 *   "channel": "STABLE",
 *   "commits": [
 *     {
 *       "sha": "e9c4141dab5ec2df7194b75b8ab34c7242c482a6",
 *       "time": "2021-12-18T19:25:03Z",
 *       "message": "Update"
 *     }
 *   ],
 *   "downloads": {
 *     "server:default": {
 *       "name": "paper-1.12.2-1620.jar",
 *       "checksums": {
 *         "sha256": "3a2041807f..."
 *       },
 *       "size": 40979699,
 *       "url": "https://.../paper-1.12.2-1620.jar"
 *     }
 *   }
 * }
 * }
 * </pre>
 */
public record BuildResponse(int id, String channel, Map<String, Download> downloads) implements Comparable<BuildResponse> {
    public static final String DEFAULT_DOWNLOAD = "server:default";

    @Override
    public int compareTo(BuildResponse o) {
        // should sort builds in descending order, latest first
        return Integer.compare(o.id, this.id);
    }

    public Channel getChannel() {
        return Channel.of(channel);
    }

    public Download getServerDownload() {
        Download download = downloads.get(DEFAULT_DOWNLOAD);
        if (download == null) {
            throw new IllegalArgumentException("Failed to find download " + DEFAULT_DOWNLOAD + " in " + this);
        }

        return download;
    }

    public record Download(String name, Map<String, String> checksums, long size, String url) {}

}

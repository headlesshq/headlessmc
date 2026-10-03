package io.github.headlesshq.headlessmc.platform.purpur.api;

import java.net.URI;

/**
 * Response object for the {@code /v2/{project}/{version}/{build}} api.
 * <pre>
 * {@code
 * {
 *   "project": "purpur",
 *   "version": "1.21.1",
 *   "build": "2329",
 *   "result": "SUCCESS",
 *   "timestamp": 1730406106810,
 *   "duration": 352888,
 *   "commits": [
 *     {
 *       "author": "granny",
 *       "email": "granny@purpurmc.org",
 *       "description": "Final 1.21.1 ...",
 *       "hash": "803bf624d9e6616b879a16e6ce3c7e196468c577",
 *       "timestamp": 1730406074000
 *     }
 *   ],
 *   "metadata": {},
 *   "md5": "dc0026462cac76c869335feb43388a76"
 * }
 * </pre>
 */
// TODO: MD5, no size is so bad wtf
public record BuildResponse(String project, String version, String build, String md5) {
    /**
     * Constructs the download URL. E.g. with {@link PurpurAPI#V2_URL}:
     * <p>{@code "https://api.purpurmc.org/v2/purpur/1.21.1/2329/download"}.
     *
     * @param v2Url the base URL, no trailing {@code "/"}
     * @return a URI pointing to the download for this build.
     */
    public URI getDownloadUrl(String v2Url) {
        return URI.create(v2Url + "/" + project + "/" + version + "/" + build + "/download");
    }

}

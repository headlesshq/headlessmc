package io.github.headlesshq.headlessmc.java.distribution.foojay;

import io.github.headlesshq.headlessmc.distribution.JavaRuntime;
import org.jspecify.annotations.Nullable;

/**
 * A package JSON object returned by the foojay disco API.
 * These are just the fields we need for our use cases.
 * <pre>
 * {@code
 * {
 *       "id": "88fb1303ff6928587c165c897f5e57d8",
 *       "archive_type": "tar.gz",
 *       "distribution": "temurin",
 *       "major_version": 25,
 *       "java_version": "25+36",
 *       "distribution_version": "25",
 *       "jdk_version": 25,
 *       "latest_build_available": false,
 *       "release_status": "ga",
 *       "term_of_support": "lts",
 *       "operating_system": "linux",
 *       "lib_c_type": "glibc",
 *       "architecture": "x64",
 *       "fpu": "unknown",
 *       "package_type": "jre",
 *       "javafx_bundled": false,
 *       "directly_downloadable": true,
 *       "filename": "OpenJDK25U-jre_x64_linux_hotspot_25_36.tar.gz",
 *       "links": {
 *         "pkg_info_uri": "https://api.foojay.io/disco/v3.0/ids/88fb1303ff6928587c165c897f5e57d8",
 *         "pkg_download_redirect": "https://api.foojay.io/disco/v3.0/ids/88fb1303ff6928587c165c897f5e57d8/redirect"
 *       },
 *       "free_use_in_production": true,
 *       "tck_tested": "yes",
 *       "tck_cert_uri": "https://adoptium.net/temurin/tck-affidavit/",
 *       "aqavit_certified": "yes",
 *       "aqavit_cert_uri": "https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25%2B36/OpenJDK25U-jre_x64_linux_hotspot_25_36.tap.zip",
 *       "size": 61362303,
 *       "feature": []
 * },
 * }
 * </pre>
 *
 * @param id       the id of the package.
 * @param filename the filename of the package.
 */
record Pkg(
    String id,
    String filename,
    @Nullable Long size,
    @Nullable String archive_type
) {
    public JavaRuntime toRuntime(String provider, int version, String distribution) {
        String name = filename;
        if (archive_type != null && filename.endsWith(archive_type)) {
            name = filename.substring(0, filename.length() - (archive_type.length() + 1));
        }

        return new JavaRuntime(
            provider,
            distribution,
            id(),
            name,
            version,
            new JavaRuntime.MetaData(size, archive_type)
        );
    }

}

package io.github.headlesshq.headlessmc.distribution;

import org.jspecify.annotations.Nullable;

public record JavaRuntime(
    String provider,
    String distribution,
    String id,
    String name,
    int version,
    @Nullable MetaData metaData
) {
    /**
     * @param size        the size of the archive in bytes.
     * @param archiveType the type of the archive, e.g. {@code tar.gz}.
     * @param javaVersion the full Java version of the runtime, e.g. {@code 21.0.5+11}.
     * @param packageType the type of the package, e.g. {@code jre} or {@code jdk}.
     */
    public record MetaData(
        @Nullable Long size,
        @Nullable String archiveType,
        @Nullable String javaVersion,
        @Nullable String packageType
    ) {}

    /**
     * @return the full Java version of this runtime, e.g. {@code 21.0.5+11}, if known.
     */
    public @Nullable String javaVersion() {
        return metaData == null ? null : metaData.javaVersion();
    }

    /**
     * @return the package type of this runtime, e.g. {@code jre}, if known.
     */
    public @Nullable String packageType() {
        return metaData == null ? null : metaData.packageType();
    }

}

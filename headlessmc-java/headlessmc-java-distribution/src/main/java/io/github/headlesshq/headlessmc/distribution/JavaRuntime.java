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
    public record MetaData(@Nullable Long size, @Nullable String archiveType) {}

}

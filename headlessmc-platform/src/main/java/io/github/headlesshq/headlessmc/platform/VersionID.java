package io.github.headlesshq.headlessmc.platform;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VersionID {
    private final boolean server;
    private final String version;
    private final String platform;
    private final String build;

}

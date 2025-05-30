package io.github.headlesshq.headlessmc.version.parser;

import io.github.headlesshq.headlessmc.version.Download;
import lombok.Data;

import java.net.URL;

@Data
final class DownloadImpl implements Download {
    private final String name;
    private final String sha1;
    private final Long size;
    private final URL url;

}

package io.github.headlesshq.headlessmc.version.parser;

import io.github.headlesshq.headlessmc.version.Assets;
import io.github.headlesshq.headlessmc.version.Download;
import lombok.Data;

@Data
final class AssetsImpl implements Assets {
    private final Download index;
    private final String name;

}

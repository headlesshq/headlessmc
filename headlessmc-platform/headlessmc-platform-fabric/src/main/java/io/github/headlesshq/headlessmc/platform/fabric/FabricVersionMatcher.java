package io.github.headlesshq.headlessmc.platform.fabric;

import io.github.headlesshq.headlessmc.platform.VanillaVersionService;
import io.github.headlesshq.headlessmc.platform.VersionService;
import io.github.headlesshq.headlessmc.platform.client.PatternVersionMatcher;
import io.github.headlesshq.headlessmc.platform.client.VersionPattern;
import io.github.headlesshq.headlessmc.version.Version;

import java.util.List;
import java.util.regex.Pattern;

/**
 * The Fabric installer always names versions {@code fabric-loader-<loader>-<mc-version>},
 * e.g. {@code fabric-loader-0.18.1-1.14.3-pre1} or {@code fabric-loader-0.4.8+build.155-1.14}.
 * Loader versions are {@code 0.18.1}, {@code 0.12.0.1} or {@code 0.4.8+build.155},
 * mc versions can contain {@code -} and spaces, e.g. {@code 1.14.3-pre1} or {@code 1.14 Pre-Release 1},
 * but do not start or end with a space.
 */
final class FabricVersionMatcher extends PatternVersionMatcher {
    static final Pattern FABRIC_LOADER = Pattern.compile(
        "fabric-loader-(?<build>\\d+(?:\\.\\d+)+(?:\\+build\\.\\d+)?)-(?<mc>\\S(?:.*\\S)?)"
    );

    private final List<VersionPattern> patterns = List.of(
        new VersionPattern(FABRIC_LOADER, (platformService, version, match) ->
            resolve(platformService, version, match.group("mc"), match.group("build")))
    );

    FabricVersionMatcher(VanillaVersionService vanillaVersionService, VersionService versionService) {
        super(vanillaVersionService, versionService);
    }

    @Override
    protected List<VersionPattern> getPatterns() {
        return patterns;
    }

    /**
     * Ids starting with {@code fabric-loader-} are reserved for the installer, if they do not match the pattern they are invalid.
     */
    @Override
    protected boolean canFallBack(Version version) {
        return !version.getId().startsWith("fabric-loader-");
    }

}

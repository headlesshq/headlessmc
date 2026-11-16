package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.stream.Collectors;

/**
 * Completes the arguments of a {@link VersionArg}: {@code [side] [platform] <mc-version> [build]}.
 * Only versions and builds that are already known to the platforms are used, this is cheap.
 */
@RequiredArgsConstructor
public class VersionArgCandidates {
    private final PlatformService platformService;

    /**
     * @param values the arguments typed so far, the last one is being completed.
     * @return candidates for the last argument.
     */
    public List<ArgCandidate> complete(@Nullable List<String> values) {
        if (values == null || values.isEmpty()) {
            return defaults();
        }

        if (values.size() == 1) {
            // first arg can be server/client/platform, or just a vanilla version
            List<ArgCandidate> result = defaults();
            result.addAll(vanillaVersions(values.getFirst()));
            return result;
        } if (values.size() == 2) {
            Side side = getSide(values.getFirst());
            if (side != null) {
                // side/<platform/vanilla-version>
                List<ArgCandidate> result = platformService.getPlatforms().stream()
                    .filter(platform -> platform.hasSupportFor(side))
                    .map(Platform::getName)
                    .map(ArgCandidate::new)
                    .collect(Collectors.toCollection(ArrayList::new));
                result.addAll(vanillaVersions(values.get(1)));
                return result;
            }

            Optional<Platform> platform = platformService.getPlatform(values.getFirst());
            if (platform.isPresent()) {
                return vanillaVersions(null);
            } else {
                return List.of(); // first arg was probably a vanilla version, nothing left to complete
            }
        } else if (values.size() == 3) {
            // side/platform/version
            Side side = getSide(values.getFirst());
            if (side != null) {
                Optional<Platform> platform = platformService.getPlatform(values.get(1));
                if (platform.isPresent() && platform.get().hasSupportFor(side)) {
                    return vanillaVersions(null);
                }

                return List.of(); // error
            }

            // platform/version/build
            return getBuilds(values, null, 0);
        } else if (values.size() == 4) {
            // side/platform/version/build
            Side side = getSide(values.getFirst());
            if (side == null) {
                return List.of(); // error
            }

            return getBuilds(values, side, 1);
        }

        return List.of(); // error
    }

    private List<ArgCandidate> getBuilds(List<String> values, @Nullable Side side, int index) {
        // platform/version/build
        Optional<Platform> platform = platformService.getPlatform(values.get(index));
        if (platform.isEmpty() || side != null && !platform.get().hasSupportFor(side)) {
            return List.of(); // error
        }

        Optional<VanillaVersion> vanillaVersion = platformService.getVanillaPlatform()
            .getVersionService()
            .getVersion(values.get(index + 1));

        //noinspection OptionalIsPresent // error
        if (vanillaVersion.isEmpty()) {
            return List.of();
        }

        return platform.map(Platform::getVersionService)
            .map(versionService -> versionService.getBuilds(vanillaVersion.get()))
            .stream()
            .flatMap(SequencedSet::stream)
            .map(version -> new ArgCandidate(version.getName()))
            .toList();
    }

    private List<ArgCandidate> vanillaVersions(@Nullable String prefix) {
        String first = prefix == null ? null : prefix.toLowerCase(Locale.ENGLISH);
        if (prefix == null
            || !first.isEmpty() && Character.isDigit(first.charAt(0))
            || first.startsWith("a")
            || first.startsWith("b")
            || first.startsWith("c")
            || first.startsWith("r")) {
            return platformService.getVanillaPlatform().getVersionService()
                .getVersions()
                .stream()
                .map(VanillaVersion::getName)
                .map(ArgCandidate::new)
                .toList();
        }

        return List.of();
    }

    private List<ArgCandidate> defaults() {
        List<ArgCandidate> result = platformService.getPlatforms().stream()
            .map(Platform::getName)
            .map(ArgCandidate::new)
            .collect(Collectors.toCollection(ArrayList::new));

        result.add(new ArgCandidate("client"));
        result.add(new ArgCandidate("server"));
        return result;
    }

    /**
     * @param string an argument.
     * @return the side the argument names, {@code null} if it is not a side.
     */
    public static @Nullable Side getSide(String string) {
        for (Side side : Side.values()) {
            if (side.name().equalsIgnoreCase(string)) {
                return side;
            }
        }

        return null;
    }

}

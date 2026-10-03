package io.github.headlesshq.headlessmc.platform.client;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionProcessor;

import java.util.Locale;
import java.util.Objects;

/**
 * This interface serves as the bridge from {@link Version} ids,
 * to {@link VersionID}s, parsing {@code "platform/mc-version/build"}, e.g.:
 * <p>{@code "1.12.2"} to {@code "vanilla/1.12.2"}</p>
 * <p>{@code "1.7.2-Forge10.12.2.1121"} to {@code "forge/1.7.2/10.12.2.1121"}</p>
 * <p>{@code "neoforge-21.10.53-beta"} to {@code "neoforge/1.21.10/53-beta"}</p>
 * <p>{@code "fabric-loader-0.18.1-1.14.3-pre1"} to {@code "fabric/1.14.3-pre1/0.18.1"}</p>
 * <p>{@code "Forge8.9.0.753"} to {@code "forge/1.6.1/8.9.0.753"}
 * (note this non-trivial example that might need a lookup of the forge-version to resolve vanilla 1.6.1)</p>
 * A {@link VersionMatcher} is bound to a {@link Platform} and
 * only parses versions for that platform (see {@link #canMatch(PlatformService, Version, VersionProcessor)}).
 *
 * @see VersionMatcherService
 */
public interface VersionMatcher {
    /**
     * Attempts to find the matching {@link VersionID} for the given {@link Version}.
     * A call to {@link #canMatch(PlatformService, Version, VersionProcessor)} should
     * be made before or else this method may throw an Exception.
     *
     * @param platformService used to resolve versions.
     * @param version         the version to match.
     * @param processor       may resolve parent versions of the version if required.
     * @return the resolved {@link VersionID} matching the version.
     * @throws HeadlessMcException if something goes wrong.
     */
    VersionID match(PlatformService platformService, Version version, VersionProcessor processor)
        throws HeadlessMcException;

    /**
     * Checks if this VersionMatcher can match ({@link #match(PlatformService, Version, VersionProcessor)})
     * the given version.
     *
     * @param platformService used to resolve versions.
     * @param version         the version to match.
     * @param processor       may resolve parent versions of the version if required.
     * @return {@code true} if {@link #match(PlatformService, Version, VersionProcessor)} can be called.
     * @throws HeadlessMcException if something goes wrong.
     */
    boolean canMatch(PlatformService platformService, Version version, VersionProcessor processor)
        throws HeadlessMcException;

    /**
     * @return the name of the Platform this VersionMatcher matches {@link VersionID}s for.
     * @see VersionID#getPlatform()
     */
    String getPlatformName();

    /**
     * Platforms don't really know each other.
     * This prevents the problems that occur if one platform contains
     * another platform e.g. {@code NeoForge} contains {@code Forge}.
     *
     * @param platformService the platform service to potentially resolve Platforms from.
     * @param version         the version to match for.
     * @param platform        platform to check for inclusion in this platform.
     * @return {@code true} if this platform contains the other platform.
     */
    default boolean includes(PlatformService platformService, Version version, Platform platform) {
        Objects.requireNonNull(platformService);
        String id = version.getId().toLowerCase(Locale.ENGLISH);
        return id.contains(getPlatformName())
            && id.contains(platform.getName())
            && !getPlatformName().equals(platform.getName())
            && getPlatformName().contains(platform.getName());
    }

}

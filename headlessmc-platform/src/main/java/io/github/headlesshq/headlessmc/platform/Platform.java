package io.github.headlesshq.headlessmc.platform;

import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
import io.github.headlesshq.headlessmc.platform.mods.ModSupport;
import io.github.headlesshq.headlessmc.platform.server.ServerSupport;
import io.github.headlesshq.headlessmc.util.maven.MavenRepository;
import io.github.headlesshq.headlessmc.version.arg.Side;

import java.util.List;
import java.util.Optional;

/**
 * Represents a (mod-loading) platform.
 * E.g. {@code Vanilla}, {@code Fabric}, {@code Forge}, {@code NeoForge},
 * {@code Paper}, or {@code Purpur}.
 * Platforms provide versions for different mc-versions via
 * {@link #getVersionService()}.
 * Platforms may provide support to install and find servers
 * via {@link #getServerSupport()}, clients via {@link #getClientSupport()}
 * or mods via {@link #getModSupport()}.
 */
public interface Platform {
    /**
     * @return the name of this platform, all lowercase, as an identifier.
     */
    String getName();

    /**
     * @return The properly capitalized name of this platform.
     */
    String getCapitalizedName();

    /**
     * @return the {@link VersionService} that manages the versions for this platform.
     */
    VersionService getVersionService();

    /**
     * @return a List of {@link MavenRepository} that libraries for this
     * platform can be downloaded from.
     */
    List<MavenRepository> getRepositories();

    /**
     * @return the {@link ServerSupport} that provides
     * {@link ServerSupport#serverFinder()}
     * and {@link ServerSupport#installer()} capabilities,
     * if this platform supports it.
     */
    Optional<ServerSupport> getServerSupport();

    /**
     * @return the {@link ClientSupport} that provides
     * {@link ClientSupport#installer()}
     * and {@link ClientSupport#versionMatcher()} capabilities,
     * if this platform supports it.
     */
    Optional<ClientSupport> getClientSupport();

    /**
     * @return the {@link ModSupport} that provides
     * {@link ModSupport#modTypes()}
     * and {@link ModSupport#modReaders()} capabilities,
     * if this platform supports it.
     */
    Optional<ModSupport> getModSupport();

    /**
     * Checks if this platform has support for the given side.
     *
     * @param side the side to check.
     * @return {@code true} if this platform has support for the given side.
     * @see #getServerSupport()
     * @see #getClientSupport()
     */
    default boolean hasSupportFor(Side side) {
        switch (side) {
            case CLIENT -> {
                return getClientSupport().isPresent();
            }
            case SERVER -> {
                return getServerSupport().isPresent();
            }
        }

        return false;
    }

}

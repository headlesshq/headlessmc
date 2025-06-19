package io.github.headlesshq.headlessmc.version.id;

import lombok.Builder;
import lombok.Data;
import lombok.With;
import org.jetbrains.annotations.Nullable;

/**
 * A VersionID identifies a launchable mc version.
 * It specifies the name of the vanilla version,
 * potentially the mod-loading platform that it
 * runs on, the build of the mod-loading platform
 * and whether it runs the server or client.
 */
@Data
@With
@Builder
public class VersionID {
    /**
     * If this targets the server, or if {@code false} the client.
     */
    private final boolean server;
    /**
     * The vanilla version name, e.g. 1.12.2, 1.21.6-pre1, 23w14a, b1.6.5, a1.0.11, or rd-132211.
     */
    private final String version;
    /**
     * The name of the platform this version runs on, e.g. fabric, forge, neoforge, paper, purpur, etc..
     * Or {@code "vanilla"} or {@code null} if vanilla.
     */
    private final @Nullable String platform; // TODO: default to Vanilla?!
    /**
     * The build of the platform this version runs on.
     * E.g. the version of the fabric-loader, 0.16.14.
     * Can be {@code null} to find any installed, or the latest version.
     * Can be {@code "latest"} for the latest version.
     */
    private final @Nullable String build;

    /**
     * Creates a new client, vanilla, VersionID for the given mc version.
     *
     * @param version the version to use.
     */
    public VersionID(String version) {
        this(version, null);
    }

    /**
     * Creates a new client VersionID for the given mc version and the given platform.
     *
     * @param version the version to use.
     * @param platform the platform to use.
     */
    public VersionID(String version, @Nullable String platform) {
        this(version, platform, false);
    }

    /**
     * Creates a new VersionID for the given mc version and the given platform.
     *
     * @param version the version to use.
     * @param platform the platform to use.
     * @param server whether the version runs on the server or not.
     */
    public VersionID(String version, @Nullable String platform, boolean server) {
        this(server, version, platform, null);
    }

    /**
     * Constructs a new VersionID.
     *
     * @param server whether this version runs on the server or not.
     * @param version the vanilla version name, see {@link #getVersion()}.
     * @param platform the platform to run on, see {@link #getPlatform()}, or {@code null} if running on vanilla.
     * @param build the build of the platform to run on, must be {@code null} if platform is {@code null}.
     */
    public VersionID(boolean server, String version, @Nullable String platform, @Nullable String build) {
        if (platform == null && build != null) {
            throw new IllegalArgumentException("Platform is required when build is specified");
        }

        this.server = server;
        this.version = version;
        this.platform = platform;
        this.build = build;
    }

    public VersionID asVanillaVersion() {
        return new VersionID(version, null, server);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        if (server) {
            str.append("server:");
        }

        if (platform != null) {
            str.append(platform).append(":");
        }

        str.append(version);
        if (build != null) {
            str.append(":").append(build);
        }

        return str.toString();
    }

}


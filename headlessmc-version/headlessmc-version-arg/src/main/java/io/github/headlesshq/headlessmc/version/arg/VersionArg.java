package io.github.headlesshq.headlessmc.version.arg;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.With;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A VersionArg identifies a launchable mc version.
 * It specifies the name of the vanilla version,
 * potentially the mod-loading platform that it
 * runs on, the build of the mod-loading platform
 * and whether it runs the server or client.
 * Examples:
 * <p>{@code "1.21.1"}
 * <p>{@code "vanilla/1.21.1"}
 * <p>{@code "fabric/1.21.1"}
 * <p>{@code "server/fabric/1.21.1"}
 * <p>{@code "client/fabric/1.21.1"}
 * <p>{@code "fabric/1.21.1/0.18.1"}
 * <p>{@code "server/fabric/1.21.1/0.18.1"}
 *
 * @param side     The {@link Side}, client/server, this VersionArg targets.
 * @param platform The name of the platform this version runs on,
 *                 e.g. vanilla, fabric, forge, neoforge, paper, purpur, etc.
 * @param version  The vanilla version name, e.g. 1.12.2, 1.21.6-pre1, 23w14a, b1.6.5, a1.0.11, or rd-132211.
 * @param build    The build of the platform this version runs on.
 *                 E.g. the version of the fabric-loader, 0.16.14.
 *                 Can be {@code null} to find the latest installed, or the latest version.
 *                 Can be {@code "latest"} for the latest version.
 */
@With
@Builder
@RegisterForReflection
public record VersionArg(Optional<Side> side, String platform, String version, Optional<String> build) {
    public static final String PLATFORM_VANILLA = "vanilla";
    public static final String LATEST = "latest";

    /**
     * Constructs a new {@link VersionArg} and performs normalization on platform
     * (lower-case, default platform vanilla if {@code null}).
     *
     * @param side     client/server this id identifies ({@link #side}).
     * @param platform the platform ({@link #platform})
     * @param version  the mc-version ({@link #version})
     * @param build    the build ({@link #build}).
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public VersionArg(Optional<Side> side, @Nullable String platform, String version, Optional<String> build) {
        //noinspection ConstantValue
        if (version == null) {
            throw new IllegalArgumentException("VersionArg.version was null");
        }

        this.side = side;
        this.platform = platform == null ? PLATFORM_VANILLA : platform.toLowerCase(Locale.ENGLISH);
        this.version = version;
        this.build = build;
    }

    /**
     * @return {@code true} if no {@link Side} has been specified.
     */
    public boolean isAnySide() {
        return side.isEmpty();
    }

    /**
     * @return {@code true} if this VersionArg targets the {@code "vanilla"} platform.
     */
    public boolean isVanilla() {
        return VersionArg.PLATFORM_VANILLA.equals(platform);
    }

    /**
     * @return {@code true} if this VersionArg targets the {@code "latest"} build.
     */
    public boolean isLatest() {
        return build.isPresent() && LATEST.equalsIgnoreCase(build.get());
    }

    /**
     * @return {@code true} if this VersionArg targets any build (latest installed build).
     */
    public boolean isAnyBuild() {
        return build.isEmpty();
    }

    /**
     * @return {@code true} if this VersionArg targets a specific build that is not {@code "latest"}.
     */
    public boolean isSpecificBuild() {
        return !isLatest() && !isAnyBuild();
    }

    /**
     * Checks if the given VersionArg targets the same
     * platform and mc-version as this VersionArg.
     *
     * @param other the id to check.
     * @return {@code true} if the given VersionArg targets the same
     * platform and mc-version as this VersionArg.
     */
    public boolean isSamePlatformVersion(VersionArg other) {
        return this.platform().equals(other.platform())
            && this.version().equals(other.version());
    }

    public VersionArg asVanillaVersion() {
        return new VersionArg(side, PLATFORM_VANILLA, version, Optional.empty());
    }

    public static VersionArg parse(List<String> strings) throws IllegalArgumentException {
        return VersionArgParser.parse(strings);
    }

    public static VersionArg parse(String... args) throws IllegalArgumentException {
        return VersionArgParser.parse(Arrays.asList(args));
    }

    public VersionArg withSide(@Nullable Side side) {
        return new VersionArg(Optional.ofNullable(side), platform(), version(), build());
    }

    public VersionArg withBuild(@Nullable String build) {
        return new VersionArg(side(), platform(), version(), Optional.ofNullable(build));
    }

    public String toString(String separator) {
        StringBuilder str = new StringBuilder();
        side.ifPresent(value -> str.append(value.toString().toLowerCase(Locale.ENGLISH)).append(separator));

        if (!PLATFORM_VANILLA.equals(platform)) {
            str.append(platform).append(separator);
        }

        str.append(version);
        build.ifPresent(string -> str.append(separator).append(string));

        return str.toString();
    }

    @Override
    public String toString() {
        return toString("/");
    }

    @SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType", "FieldMayBeFinal"})
    public static class VersionArgBuilder {
        // set the Default values for these builder parameters
        private Optional<Side> side = Optional.empty();
        private Optional<String> build = Optional.empty();

        public VersionArgBuilder onSide(@Nullable Side side) {
            return this.side(Optional.ofNullable(side));
        }

        public VersionArgBuilder withBuild(@Nullable String build) {
            return this.build(Optional.ofNullable(build));
        }
    }

}

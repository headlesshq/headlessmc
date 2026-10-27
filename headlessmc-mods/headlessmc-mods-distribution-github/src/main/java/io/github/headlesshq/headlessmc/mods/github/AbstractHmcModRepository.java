package io.github.headlesshq.headlessmc.mods.github;

import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

/**
 * Base for the mods of the headlesshq organization that are built for multiple versions and loaders,
 * like {@code hmc-specifics}, {@code hmc-optimizations} and {@code mc-runtime-test}.
 * They publish one release per mc-version and mod version, tagged {@code <mc-version>-<mod-version>},
 * and a release tagged {@code <mc-version>-latest}, which always contains the latest build.
 * Each release contains one file per loader, named {@code <name>-<mc-version>-<loader>-<mod-version>.jar},
 * e.g. {@code hmc-specifics-1.21.1-fabric-latest.jar} or {@code hmc-specifics-1.21.1-forge-3.0.1.jar}.
 */
public abstract class AbstractHmcModRepository implements ModRepository {
    /**
     * The owner of the repositories.
     */
    public static final String OWNER = "headlesshq";

    /**
     * The mod version of the releases that contain the latest build for an mc-version.
     */
    public static final String LATEST = "latest";

    /**
     * The HeadlessMc platforms the mods are built for, which are also the loader names used in the release files.
     */
    static final Set<String> LOADERS = Set.of("fabric", "forge", "neoforge");

    private final String name;
    private final String repository;
    private final String description;
    private final @Nullable String modId;

    /**
     * Constructs a new AbstractHmcModRepository.
     *
     * @param name        the name of the mod, which is also the prefix of the release files.
     * @param repository  the name of the GitHub repository.
     * @param description a description of what the mod does.
     * @param modId       the id the mod declares in its metadata, {@code null} if it is unknown.
     */
    protected AbstractHmcModRepository(String name, String repository, String description, @Nullable String modId) {
        this.name = name;
        this.repository = repository;
        this.description = description;
        this.modId = modId;
    }

    @Override
    public boolean supports(VersionArg version) {
        return LOADERS.contains(version.platform());
    }

    @Override
    public Optional<String> getReleaseTag(VersionArg version, @Nullable String modVersion) {
        return Optional.of(version.version() + "-" + (modVersion == null ? LATEST : modVersion));
    }

    @Override
    public Optional<GitHubRelease.Asset> findAsset(GitHubRelease release, VersionArg version) {
        String tagPrefix = version.version() + "-";
        if (!supports(version) || !release.tagName().startsWith(tagPrefix)) {
            return Optional.empty();
        }

        String modVersion = release.tagName().substring(tagPrefix.length());
        return release.getAsset(
            "%s-%s-%s-%s.jar".formatted(name, version.version(), version.platform(), modVersion)
        );
    }

    @Override
    public Optional<String> getModId() {
        return Optional.ofNullable(modId);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getOwner() {
        return OWNER;
    }

    @Override
    public String getRepository() {
        return repository;
    }

}

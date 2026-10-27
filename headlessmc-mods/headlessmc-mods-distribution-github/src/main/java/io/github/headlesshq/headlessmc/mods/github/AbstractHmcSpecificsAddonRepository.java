package io.github.headlesshq.headlessmc.mods.github;

import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Base for mods that build on top of {@code hmc-specifics}.
 * Their releases contain a single version independent file, named {@code <file-prefix>-<release-tag>.jar}.
 * They support all versions {@code hmc-specifics} supports,
 * which is why {@code hmc-specifics} is always downloaded alongside them.
 * By default, the latest release of the repository is used, a specific mod version is the tag of a release.
 */
public abstract class AbstractHmcSpecificsAddonRepository implements ModRepository {
    private final String name;
    private final String repository;
    private final String filePrefix;
    private final String description;
    private final @Nullable String modId;

    /**
     * Constructs a new AbstractHmcSpecificsAddonRepository.
     *
     * @param name        the name of the mod in HeadlessMc.
     * @param repository  the name of the GitHub repository.
     * @param filePrefix  the name of the release file without the {@code -<release-tag>.jar} suffix.
     * @param description a description of what the mod does.
     * @param modId       the id the mod declares in its metadata, {@code null} if it is unknown.
     */
    protected AbstractHmcSpecificsAddonRepository(
        String name,
        String repository,
        String filePrefix,
        String description,
        @Nullable String modId
    ) {
        this.name = name;
        this.repository = repository;
        this.filePrefix = filePrefix;
        this.description = description;
        this.modId = modId;
    }

    @Override
    public boolean supports(VersionArg version) {
        return AbstractHmcModRepository.LOADERS.contains(version.platform());
    }

    @Override
    public Optional<String> getReleaseTag(VersionArg version, @Nullable String modVersion) {
        return Optional.ofNullable(modVersion);
    }

    @Override
    public Optional<GitHubRelease.Asset> findAsset(GitHubRelease release, VersionArg version) {
        return release.getAsset(filePrefix + "-" + release.tagName() + ".jar");
    }

    @Override
    public List<String> getDependencies() {
        return List.of(HmcSpecificsRepository.NAME);
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
        return AbstractHmcModRepository.OWNER;
    }

    @Override
    public String getRepository() {
        return repository;
    }

}

package io.github.headlesshq.headlessmc.platform.client;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.*;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionProcessor;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

/**
 * A {@link VersionMatcher} for platforms that name the versions they install in known formats,
 * e.g. {@code fabric-loader-<loader>-<mc-version>}.
 * Each format is a {@link VersionPattern} that has to match the entire version id,
 * the matched groups are then looked up exactly, instead of searching the id for
 * version names it contains, like the {@link DefaultVersionMatcher} does.
 * <p>
 * If a version id matches one of the patterns, the result is final:
 * either the groups name a version that exists on this platform, or this platform does not match the version.
 * This allows platforms with overlapping formats, e.g. Forge and NeoForge both use
 * {@code 1.20.1-forge-<build>}, to only match the builds they actually have.
 * Only version ids that match none of the patterns, e.g. versions installed under a custom name,
 * fall back to the matching of the {@link DefaultVersionMatcher},
 * unless {@link #canFallBack(Version)} forbids it, e.g. for malformed ids that use the prefix of the platform.
 */
public abstract class PatternVersionMatcher extends DefaultVersionMatcher {
    public PatternVersionMatcher(VanillaVersionService vanillaVersionService, VersionService versionService) {
        super(vanillaVersionService, versionService);
    }

    /**
     * @return the formats of the version ids of this platform, the first pattern that resolves wins.
     */
    protected abstract List<VersionPattern> getPatterns();

    @Override
    public VersionID match(
        PlatformService platformService,
        Version version,
        VersionProcessor processor
    ) throws HeadlessMcException {
        PatternMatch match = matchPatterns(platformService, version);
        if (match.id().isPresent()) {
            return match.id().get();
        }

        if (match.matchedPattern()) {
            throw new VersionMatchException(
                "%s matches the format of %s versions, but does not name an existing %s version".formatted(
                    version.getId(),
                    getPlatformName(),
                    getPlatformName()
                )
            );
        }

        if (!canFallBack(version)) {
            throw new VersionMatchException(
                "%s does not match the format of %s versions".formatted(version.getId(), getPlatformName())
            );
        }

        return super.match(platformService, version, processor);
    }

    @Override
    public boolean canMatch(
        PlatformService platformService,
        Version version,
        VersionProcessor processor
    ) throws HeadlessMcException {
        PatternMatch match = matchPatterns(platformService, version);
        if (match.matchedPattern()) {
            return match.id().isPresent();
        }

        return canFallBack(version) && super.canMatch(platformService, version, processor);
    }

    /**
     * Called for versions whose id matches none of the {@link #getPatterns()}.
     *
     * @param version the version that is being matched.
     * @return {@code true} if the version may be matched by the {@link DefaultVersionMatcher}.
     */
    protected boolean canFallBack(Version version) {
        return true;
    }

    /**
     * Matches the id of the given version against all {@link #getPatterns()}.
     *
     * @param platformService used to resolve versions.
     * @param version         the version to match.
     * @return the result of the first pattern that matched and resolved,
     * or whether any pattern matched at all.
     * @throws HeadlessMcException if resolving a version fails.
     */
    protected PatternMatch matchPatterns(PlatformService platformService, Version version) throws HeadlessMcException {
        boolean matchedPattern = false;
        for (VersionPattern pattern : getPatterns()) {
            Matcher matcher = pattern.pattern().matcher(version.getId());
            if (matcher.matches()) {
                matchedPattern = true;
                Optional<VersionID> id = pattern.resolver().resolve(platformService, version, matcher.toMatchResult());
                if (id.isPresent()) {
                    return new PatternMatch(true, id);
                }
            }
        }

        return new PatternMatch(matchedPattern, Optional.empty());
    }

    /**
     * Resolves a build of this platform for a mc version, both names have to match exactly.
     * If the version inherits from a vanilla version, that is the mc version the version is launched with,
     * so it is preferred over the mc version in the id.
     *
     * @param platformService used to resolve versions.
     * @param version         the version that is being matched.
     * @param mcVersion       the name of the vanilla version in the id.
     * @param build           the name of the build of this platform.
     * @return the {@link VersionID} or none if the vanilla version or the build do not exist.
     * @throws HeadlessMcException if something goes wrong.
     */
    protected Optional<VersionID> resolve(
        PlatformService platformService,
        Version version,
        String mcVersion,
        String build
    ) throws HeadlessMcException {
        Optional<VanillaVersion> parent = getVanillaParent(version);
        Optional<VanillaVersion> vanillaVersion = parent.isPresent()
            ? parent
            : vanillaVersionService.getVersion(mcVersion);
        if (vanillaVersion.isEmpty()) {
            return Optional.empty();
        }

        Optional<? extends PlatformVersion> platformVersion = versionService.getBuild(vanillaVersion.get(), build);
        return platformVersion.map(pV -> createVersionId(platformService, vanillaVersion.get(), pV));
    }

    /**
     * Resolves a build of this platform whose id does not contain the mc version.
     * This only works for {@link BoundPlatformVersion}s, as the build has to tell us its mc version.
     * If the version inherits from a vanilla version, only builds for that mc version are considered.
     *
     * @param platformService used to resolve versions.
     * @param version         the version that is being matched.
     * @param build           the name of the build of this platform.
     * @return the {@link VersionID} or none if no build, or no unique build has that name.
     * @throws HeadlessMcException if something goes wrong.
     */
    protected Optional<VersionID> resolveBuild(
        PlatformService platformService,
        Version version,
        String build
    ) throws HeadlessMcException {
        return resolveUnique(platformService, version, versionService.getVersions().stream()
            .filter(platformVersion -> platformVersion instanceof BoundPlatformVersion)
            .map(BoundPlatformVersion.class::cast)
            .filter(platformVersion -> platformVersion.getName().equals(build))
            .toList());
    }

    /**
     * @param platformService used to resolve versions.
     * @param version         the version that is being matched.
     * @param candidates      the builds the version could be.
     * @return the {@link VersionID} of the only candidate for the vanilla version the version inherits from,
     * or of the only candidate if it does not inherit from a vanilla version.
     * @throws HeadlessMcException if something goes wrong.
     */
    protected Optional<VersionID> resolveUnique(
        PlatformService platformService,
        Version version,
        List<BoundPlatformVersion> candidates
    ) throws HeadlessMcException {
        Optional<VanillaVersion> parent = getVanillaParent(version);
        if (parent.isPresent()) {
            candidates = candidates.stream()
                .filter(candidate -> candidate.getVanillaVersion().equals(parent.get().getName()))
                .toList();
        }

        if (candidates.size() != 1) {
            return Optional.empty();
        }

        BoundPlatformVersion build = candidates.getFirst();
        return Optional.of(createVersionId(platformService, build.getVanillaVersion(vanillaVersionService), build));
    }

    /**
     * @param version the version that is being matched.
     * @return the vanilla version the version inherits from, none if it has no parent,
     * or the parent is not a vanilla version, e.g. a custom version inheriting from a modded version.
     * @throws HeadlessMcException if something goes wrong.
     */
    protected Optional<VanillaVersion> getVanillaParent(Version version) throws HeadlessMcException {
        String parent = version.getInheritsFrom();
        return parent == null ? Optional.empty() : vanillaVersionService.getVersion(parent);
    }

    /**
     * The result of {@link #matchPatterns(PlatformService, Version)}.
     *
     * @param matchedPattern if the version id matched any pattern.
     * @param id             the resolved {@link VersionID}.
     */
    protected record PatternMatch(boolean matchedPattern, Optional<VersionID> id) {

    }

}

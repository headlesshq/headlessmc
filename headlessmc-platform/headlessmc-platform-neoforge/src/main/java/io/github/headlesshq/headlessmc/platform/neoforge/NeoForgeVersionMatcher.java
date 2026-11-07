package io.github.headlesshq.headlessmc.platform.neoforge;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.*;
import io.github.headlesshq.headlessmc.platform.client.PatternVersionMatcher;
import io.github.headlesshq.headlessmc.platform.client.VersionPattern;
import io.github.headlesshq.headlessmc.platform.forge.Forge;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionProcessor;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * NeoForge names its versions {@code neoforge-<neoforge-version>}, e.g. {@code neoforge-21.1.240},
 * {@code neoforge-26.1.0.1-beta} or {@code neoforge-0.25w14craftmine.3-beta}.
 * The {@link NeoForgeVersionService} strips the mc version from the NeoForge version to get the build name,
 * e.g. {@code 21.1.240} is build {@code 240} for {@code 1.21.1}, this matcher reverses that.
 * <p>
 * When NeoForge was freshly forked the versions for mc-version {@code 1.20.1}
 * still followed the {@link Forge} naming scheme {@code 1.20.1-forge-<build>}.
 * Such versions are only matched if they name a NeoForge build.
 *
 * @see NeoForgeArtifactResolver
 */
public class NeoForgeVersionMatcher extends PatternVersionMatcher {
    static final Pattern NEOFORGE = Pattern.compile("neoforge-(?<version>\\d[\\w.+-]*\\w)");
    static final Pattern LEGACY_FORGE = Pattern.compile("(?<mc>1\\.20\\.1)-forge-(?<build>\\d+(?:\\.\\d+)+)");

    private final List<VersionPattern> patterns = List.of(
        new VersionPattern(NEOFORGE, (platformService, version, match) ->
            resolveNeoForgeVersion(platformService, version, match.group("version"))),
        new VersionPattern(LEGACY_FORGE, (platformService, version, match) ->
            resolve(platformService, version, match.group("mc"), match.group("build")))
    );

    public NeoForgeVersionMatcher(VanillaVersionService vanillaVersionService, VersionService versionService) {
        super(vanillaVersionService, versionService);
    }

    @Override
    protected List<VersionPattern> getPatterns() {
        return patterns;
    }

    /**
     * Ids starting with {@code neoforge-} are reserved for the installer, if they do not match the pattern they are invalid.
     */
    @Override
    protected boolean canFallBack(Version version) {
        return !version.getId().startsWith("neoforge-");
    }

    private Optional<VersionID> resolveNeoForgeVersion(
        PlatformService platformService,
        Version version,
        String neoForgeVersion
    ) throws HeadlessMcException {
        return resolveUnique(platformService, version, versionService.getVersions().stream()
            .filter(platformVersion -> platformVersion instanceof BoundPlatformVersion)
            .map(BoundPlatformVersion.class::cast)
            .filter(build -> NeoForgeVersionService.getBuildName(neoForgeVersion, build.getVanillaVersion())
                .equals(build.getName()))
            .toList());
    }

    @Override
    protected VersionID onAmbiguousPlatformMatches(
        NameMatchResult<? extends PlatformVersion> match,
        PlatformService platformService,
        Version version,
        VersionProcessor processor,
        VanillaVersion vanillaVersion,
        List<BoundPlatformVersion> boundPlatformMatch
    ) {
        /*
        Failed to determine version neoforge-20.6.21-beta, vanilla version: 1.20.6,
        filtered matches: [neoforge/1.20.6/21-beta, neoforge/1.20.6/1-beta],
        */
        for (BoundPlatformVersion boundPlatformVersion : boundPlatformMatch) {
            String vanillaVersionName = boundPlatformVersion.getVanillaVersion().startsWith("1.")
                ? boundPlatformVersion.getVanillaVersion().substring(2)
                : boundPlatformVersion.getVanillaVersion();

            if (version.getId().endsWith(vanillaVersionName + "." + boundPlatformVersion.getName())) {
                return createVersionId(platformService, vanillaVersion, boundPlatformVersion);
            }
        }

        return super.onAmbiguousPlatformMatches(
            match,
            platformService,
            version,
            processor,
            vanillaVersion,
            boundPlatformMatch
        );
    }

}

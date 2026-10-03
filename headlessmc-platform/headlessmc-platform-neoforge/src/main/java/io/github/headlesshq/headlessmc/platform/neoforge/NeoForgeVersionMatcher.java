package io.github.headlesshq.headlessmc.platform.neoforge;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.*;
import io.github.headlesshq.headlessmc.platform.client.DefaultVersionMatcher;
import io.github.headlesshq.headlessmc.platform.client.VersionMatcher;
import io.github.headlesshq.headlessmc.platform.forge.Forge;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * When NeoForge was freshly forked the versions for mc-version {@code 1.20.1}
 * still followed the {@link Forge} naming scheme.
 * This {@link VersionMatcher} allows matches such versions.
 *
 * @see NeoForgeArtifactResolver
 */
@Getter
@NeoForge
@ApplicationScoped
@RequiredArgsConstructor
public class NeoForgeVersionMatcher implements VersionMatcher {
    private final VersionMatcher defaultVersionMatcher;
    private final String platformName;

    @Inject
    public NeoForgeVersionMatcher(
        VanillaVersionService vanillaVersionService,
        @NeoForge VersionService versionService
    ) {
        this(new NeoForgeDefaultVersionMatcher(vanillaVersionService, versionService), NeoForge.PLATFORM_NAME);
    }

    @Override
    public VersionID match(PlatformService platformService, Version version, VersionProcessor processor) {
        return defaultVersionMatcher.match(platformService, version, processor);
    }

    @Override
    public boolean canMatch(PlatformService platformService, Version version, VersionProcessor processor) {
        String id = version.getId().toLowerCase(Locale.ENGLISH);
        if (id.startsWith("1.20.1-forge")) {
            return tryMatch(platformService, version, processor).isPresent();
        }

        return defaultVersionMatcher.canMatch(platformService, version, processor);
    }

    private Optional<VersionID> tryMatch(PlatformService platformService, Version version, VersionProcessor processor) {
        try {
            return Optional.of(defaultVersionMatcher.match(platformService, version, processor));
        } catch (HeadlessMcException ignored) {
            return Optional.empty();
        }
    }

    private static final class NeoForgeDefaultVersionMatcher extends DefaultVersionMatcher {
        public NeoForgeDefaultVersionMatcher(
            VanillaVersionService vanillaVersionService,
            VersionService versionService
        ) {
            super(vanillaVersionService, versionService);
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
                    return createVersionId(platformService, vanillaVersion, boundPlatformMatch.getFirst());
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

}

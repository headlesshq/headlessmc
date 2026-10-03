package io.github.headlesshq.headlessmc.platform.client;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.*;
import io.github.headlesshq.headlessmc.version.ProcessedVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionProcessor;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class DefaultVersionMatcher extends AbstractVersionMatcher implements VersionMatcher {
    protected final VanillaVersionService vanillaVersionService;
    protected final VersionService versionService;

    @Override
    public VersionID match(
        PlatformService platformService,
        Version version,
        VersionProcessor processor
    ) throws HeadlessMcException {
        VanillaVersion exactVanillaMatch;
        if ((exactVanillaMatch = vanillaVersionService.getVersion(version.getId()).orElse(null)) != null) {
            return createVersionId(platformService, exactVanillaMatch, null);
        }

        NameMatchResult<? extends PlatformVersion> match = match(
            // TODO: this might not always be feasible, or good, e.g. on Purpur,
            //  instead try to get VanillaVersion first and filter builds???
            versionService.getVersions(),
            version
        );

        if (match.versions().isEmpty()) {
            throw new VersionMatchException("Failed to find platform version for " + version + " on " + getPlatformName());
        }

        if (match.isAmbiguous()) {
            return resolveAmbiguousMatch(match, platformService, version, processor);
        }

        PlatformVersion first = match.versions().getFirst();
        if (first instanceof VanillaVersion vanillaVersion) {
            return createVersionId(platformService, vanillaVersion, null);
        }

        if (first instanceof BoundPlatformVersion boundPlatformVersion) {
            VanillaVersion vanillaVersion = boundPlatformVersion.getVanillaVersion(vanillaVersionService);
            return createVersionId(platformService, vanillaVersion, first);
        }

        VanillaVersion vanillaVersion = getVanillaVersion(version, processor);
        return createVersionId(platformService, vanillaVersion, first);
    }

    protected VersionID resolveAmbiguousMatch(
        NameMatchResult<? extends PlatformVersion> match,
        PlatformService platformService,
        Version version,
        VersionProcessor processor
    ) {
        VanillaVersion vanillaVersion = getVanillaVersion(version, processor);
        List<BoundPlatformVersion> boundPlatformMatch = new ArrayList<>(1);
        for (PlatformVersion platformVersion : match.versions()) {
            if (platformVersion instanceof BoundPlatformVersion boundVersion
                && boundVersion.getVanillaVersion().equals(vanillaVersion.getName())) {
                boundPlatformMatch.add(boundVersion);
            }
        }

        if (boundPlatformMatch.size() == 1) {
            return createVersionId(platformService, vanillaVersion, boundPlatformMatch.getFirst());
        }

        return onAmbiguousPlatformMatches(match, platformService, version, processor, vanillaVersion, boundPlatformMatch);
    }

    protected VersionID onAmbiguousPlatformMatches(
        NameMatchResult<? extends PlatformVersion> match,
        PlatformService platformService,
        Version version,
        VersionProcessor processor,
        VanillaVersion vanillaVersion,
        List<BoundPlatformVersion> boundPlatformMatch
    ) {
        throw new VersionMatchException(
            "Failed to determine version %s, vanilla version: %s, filtered matches: %s, matches: %s".formatted(
                version.getId(),
                vanillaVersion,
                boundPlatformMatch,
                match.versions()
            )
        );
    }

    protected VanillaVersion getVanillaVersion(Version version, VersionProcessor processor) throws HeadlessMcException {
        NameMatchResult<VanillaVersion> vanilla;
        // could be dangerous: neoforge-21.1.156 contains 1.15
        String parent = version.getInheritsFrom();
        if (parent != null) {
            vanilla = match(vanillaVersionService.getVersions(), parent.toLowerCase(Locale.ENGLISH));
            if (vanilla.isValid()) {
                return vanilla.versions().getFirst();
            }
        }

        ProcessedVersion processedVersion = processor.process(version);
        List<Version> hierarchy = processedVersion.hierarchy().reversed();
        for (int i = 1; i < hierarchy.size(); i++) {
            Version current = hierarchy.get(i);
            vanilla = match(vanillaVersionService.getVersions(), current);
            if (vanilla.isValid()) {
                return vanilla.versions().getFirst();
            }
        }

        // can be dangerous: neoforge-21.1.156 contains 1.15
        vanilla = match(vanillaVersionService.getVersions(), version);
        if (vanilla.isValid()) {
            return vanilla.versions().getFirst();
        }

        throw new VersionMatchException("Failed to determine vanilla version for " + version.getId());
    }

    @Override
    public boolean canMatch(
        PlatformService platformService,
        Version version,
        VersionProcessor processor
    ) throws HeadlessMcException {
        String id = version.getId().toLowerCase(Locale.ENGLISH);
        return id.contains(getPlatformName());
    }

    @Override
    public String getPlatformName() {
        return versionService.getPlatformName();
    }

}

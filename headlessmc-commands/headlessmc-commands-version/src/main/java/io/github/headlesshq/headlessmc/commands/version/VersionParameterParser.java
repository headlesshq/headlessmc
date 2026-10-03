package io.github.headlesshq.headlessmc.commands.version;

import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.version.arg.Side;

import java.util.*;

final class VersionParameterParser {
    public static VersionSearchParameter parse(PlatformService platformService, List<String> parameters) {
        Set<Side> sides = new HashSet<>();
        Set<Platform> platforms = new HashSet<>();
        Set<VanillaVersion> versions = new HashSet<>();
        Set<String> other = new HashSet<>();
        for (String param : parameters) {
            String id = param.toLowerCase(Locale.ENGLISH);
            List<Platform> foundPlatforms = platformService.getPlatforms().stream()
                .filter(platform -> platform.getName().toLowerCase(Locale.ENGLISH).contains(id))
                .toList();
            if (!foundPlatforms.isEmpty()) {
                platforms.addAll(foundPlatforms);
                continue;
            }

            try {
                sides.add(Side.valueOf(param.toUpperCase(Locale.ENGLISH)));
                continue;
            } catch (IllegalArgumentException ignored) {}

            Optional<VanillaVersion> version = platformService.getVanillaPlatform().getVersionService().getVersion(id);
            if (version.isPresent()) {
                versions.add(version.get());
                continue;
            }

            other.add(id);
        }

        if (sides.isEmpty()) {
            sides.addAll(Arrays.asList(Side.values()));
        }

        if (platforms.isEmpty()) {
            platforms.addAll(platformService.getPlatformsWithoutVanilla());
        }

        if (versions.isEmpty()) {
            versions.addAll(platformService.getVanillaPlatform().getVersionService().getVersions());
        }

        // TODO: handle "latest"

        return new VersionSearchParameter(sides, platforms, versions, other);
    }

}

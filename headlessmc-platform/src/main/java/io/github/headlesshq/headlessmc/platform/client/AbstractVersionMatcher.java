package io.github.headlesshq.headlessmc.platform.client;

import io.github.headlesshq.headlessmc.platform.*;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.SequencedSet;

@RequiredArgsConstructor
public abstract class AbstractVersionMatcher implements VersionMatcher {
    protected <V extends PlatformVersion> DefaultVersionMatcher.NameMatchResult<V> match(
        SequencedSet<V> versions,
        Version version
    ) {
        String id = version.getId().toLowerCase(Locale.ENGLISH);
        return match(versions, id);
    }

    protected <V extends PlatformVersion> DefaultVersionMatcher.NameMatchResult<V> match(
        SequencedSet<V> versions,
        String id
    ) {
        List<V> result = versions.stream()
            .filter(platformVersion -> id.contains(platformVersion.getName()))
            .sorted(Comparator.comparingInt(pV -> -pV.getName().length()))
            .toList();

        return new DefaultVersionMatcher.NameMatchResult<>(result);
    }

    protected final VersionID createVersionId(
        PlatformService platformService,
        VanillaVersion version,
        @Nullable PlatformVersion platformVersion) {
        return VersionID.resolve(platformService, VersionArg.builder()
            .version(version.getName())
            .platform(platformVersion == null ? Vanilla.PLATFORM_NAME : platformVersion.getPlatformName())
            .withBuild(platformVersion == null ? null : platformVersion.getName())
            .build()
        );
    }

    protected record NameMatchResult<V extends PlatformVersion>(List<V> versions) {
        boolean isValid() {
            return !versions.isEmpty() && !isAmbiguous();
        }

        boolean isAmbiguous() {
            return versions.size() > 1 && versions.get(0).getName().length() == versions.get(1).getName().length();
        }
    }

}

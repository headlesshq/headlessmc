package io.github.headlesshq.headlessmc.updater;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * The latest release of HeadlessMc, as cached by the {@link LatestReleaseCache}.
 *
 * @param repository the repository the release has been fetched from, see {@link UpdaterConfig#repository()}.
 * @param release    the latest release.
 */
@RegisterForReflection
public record LatestRelease(String repository, GitHubRelease release) implements ReflectionRegistered {

}

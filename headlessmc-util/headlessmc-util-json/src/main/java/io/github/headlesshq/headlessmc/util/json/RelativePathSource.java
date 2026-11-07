package io.github.headlesshq.headlessmc.util.json;

import java.nio.file.Path;

/**
 * A well known location, e.g. {@code user.home} or the {@code .minecraft} directory,
 * that serialized paths can be stored relative to.
 * This way files written by HeadlessMc stay valid when they are moved to another machine,
 * where these locations resolve to different absolute paths.
 * Implementations are discovered via CDI by the {@link RelativePathService}.
 */
public interface RelativePathSource {
    /**
     * @return a stable identifier for this location, written into serialized paths.
     */
    String getIdentifier();

    /**
     * @return the path this location currently points to.
     */
    Path getPath();

}

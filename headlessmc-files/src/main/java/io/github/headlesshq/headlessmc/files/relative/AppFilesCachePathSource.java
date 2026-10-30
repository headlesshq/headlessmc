package io.github.headlesshq.headlessmc.files.relative;

import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.util.json.RelativePathSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;

/**
 * Paths relative to the HeadlessMc cache directory, see {@link AppFiles#getCacheDir()}.
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class AppFilesCachePathSource implements RelativePathSource {
    public static final String IDENTIFIER = "headlessmc.cache";

    private final AppFiles appFiles;

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Path getPath() {
        return appFiles.getCacheDir();
    }

}

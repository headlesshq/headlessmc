package io.github.headlesshq.headlessmc.files.relative;

import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.util.json.RelativePathSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;

/**
 * Paths relative to the HeadlessMc data directory, see {@link AppFiles#getDataDir()}.
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class AppFilesDataPathSource implements RelativePathSource {
    public static final String IDENTIFIER = "headlessmc.data";

    private final AppFiles appFiles;

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Path getPath() {
        return appFiles.getDataDir();
    }

}

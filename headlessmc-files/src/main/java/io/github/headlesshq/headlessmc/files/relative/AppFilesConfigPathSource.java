package io.github.headlesshq.headlessmc.files.relative;

import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.util.json.RelativePathSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;

/**
 * Paths relative to the HeadlessMc config directory, see {@link AppFiles#getConfigDir()}.
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class AppFilesConfigPathSource implements RelativePathSource {
    public static final String IDENTIFIER = "headlessmc.config";

    private final AppFiles appFiles;

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Path getPath() {
        return appFiles.getConfigDir();
    }

}

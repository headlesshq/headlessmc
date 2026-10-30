package io.github.headlesshq.headlessmc.files.relative;

import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.util.json.RelativePathSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;

/**
 * Paths relative to the {@code .minecraft} directory, see {@link McFiles#getMcDir()}.
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class McFilesPathSource implements RelativePathSource {
    public static final String IDENTIFIER = "minecraft";

    private final McFiles mcFiles;

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Path getPath() {
        return mcFiles.getMcDir();
    }

}

package io.github.headlesshq.headlessmc.files.relative;

import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.util.json.RelativePathSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;

/**
 * Paths relative to the primary {@code .minecraft} directory, see {@link McFilesService#getPrimary()}
 * and {@link McFiles#getMcDir()}.
 * Additional {@code .minecraft} directories have no identifier, they are identified by their path,
 * so paths inside of them are written as absolute paths.
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class McFilesPathSource implements RelativePathSource {
    public static final String IDENTIFIER = "minecraft";

    private final McFilesService mcFilesService;

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Path getPath() {
        return mcFilesService.getPrimary().getMcDir();
    }

}

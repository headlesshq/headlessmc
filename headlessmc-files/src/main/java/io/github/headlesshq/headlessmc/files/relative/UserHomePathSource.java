package io.github.headlesshq.headlessmc.files.relative;

import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.util.json.RelativePathSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;

/**
 * Paths relative to the {@code user.home} directory.
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class UserHomePathSource implements RelativePathSource {
    public static final String IDENTIFIER = "user.home";

    private final FileService fileService;

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Path getPath() {
        String userHome = System.getProperty("user.home");
        return userHome == null ? fileService.getUserPath() : fileService.getUserPath(userHome);
    }

}

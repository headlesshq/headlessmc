package io.github.headlesshq.headlessmc.java.sources;

import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

/**
 * {@code ~/.jdks} is where e.g. IntelliJ installs JDKs to.
 */
@Slf4j
@ApplicationScoped
@Named("java:source:jdks")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class JdksSource implements JavaSource {
    private final JavaScannerService javaScannerService;

    @Override
    public String getName() {
        return "user.home.jdks";
    }

    @Override
    public List<Java> getJavas() {
        try {
            Path path = SystemUtils.getUserHomePath().resolve(".jdks");
            return javaScannerService.scanSubDirsOf(this, path);
        } catch (NullPointerException | InvalidPathException e) {
            log.error("Failed to get Java versions from ~/.jdks", e);
            return List.of();
        }
    }

    @Override
    public int sort() {
        return JavaSource.SORT_DEFAULT;
    }

}

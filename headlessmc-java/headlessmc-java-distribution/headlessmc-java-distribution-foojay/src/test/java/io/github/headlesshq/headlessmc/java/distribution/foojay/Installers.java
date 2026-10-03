package io.github.headlesshq.headlessmc.java.distribution.foojay;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.java.JavaConfig;
import io.github.headlesshq.headlessmc.java.sources.JavaExecutableFinder;
import io.github.headlesshq.headlessmc.java.sources.JavaHomeFinder;
import io.github.headlesshq.headlessmc.os.OS;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Builds {@link FoojayInstallerService}s for the installer and provider tests. */
final class Installers {
    private Installers() {
    }

    static FoojayInstallerService installer(OS os) {
        JavaExecutableFinder executableFinder = new JavaExecutableFinder(os);
        return new FoojayInstallerService(
            executableFinder,
            new JavaHomeFinder(executableFinder, javaConfig()),
            new DefaultFileService(new DefaultFileSystemProvider()),
            os
        );
    }

    private static Holder<JavaConfig> javaConfig() {
        JavaConfig config = new JavaConfig() {
            @Override
            public Optional<List<Path>> versions() {
                return Optional.empty();
            }

            @Override
            public boolean failOnParsingFailure() {
                return false;
            }

            @Override
            public boolean download() {
                return true;
            }

            @Override
            public int maxScanDepth() {
                return 6;
            }
        };
        return () -> config;
    }

}

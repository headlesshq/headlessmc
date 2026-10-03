package io.github.headlesshq.headlessmc.java.sources;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.java.JavaConfig;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** A {@link JavaConfig} holding the given versions, with the defaults for everything else. */
final class FakeJavaConfig {
    static final int MAX_SCAN_DEPTH = 6;

    private FakeJavaConfig() {
    }

    static Holder<JavaConfig> holder() {
        return holder(List.of());
    }

    static Holder<JavaConfig> holder(List<Path> versions) {
        JavaConfig config = new JavaConfig() {
            @Override
            public Optional<List<Path>> versions() {
                return versions.isEmpty() ? Optional.empty() : Optional.of(versions);
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
                return MAX_SCAN_DEPTH;
            }
        };
        return () -> config;
    }

}

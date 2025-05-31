package io.github.headlesshq.headlessmc.version;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

public interface Extractor {
    Extractor NO_EXTRACTION = (from, to) -> {};

    void extract(Path from, Path to) throws IOException;

    default boolean isExtracting() {
        return false;
    }

    default boolean shouldExtract(@Nullable String name) {
        return false;
    }

    static Extractor of(@Nullable ExtractionRules rules) {
        if (rules == null) {
            return NO_EXTRACTION;
        }

        return new ExtractorImpl(rules.getExclusions());
    }

}

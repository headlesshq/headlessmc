package io.github.headlesshq.headlessmc.version;

import org.jetbrains.annotations.Nullable;

public interface Library extends Download {
    Rule getRule();

    @Nullable ExtractionRules getExtractionRules();

}

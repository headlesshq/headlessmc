package io.github.headlesshq.headlessmc.version;

import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public interface ExtractionRules {
    @Unmodifiable
    List<String> getExclusions();

}

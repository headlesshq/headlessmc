package io.github.headlesshq.headlessmc.version.parser;

import io.github.headlesshq.headlessmc.version.ExtractionRules;
import lombok.Data;

import java.util.List;

@Data
final class ExtractionRulesImpl implements ExtractionRules {
    private final List<String> exclusions;

}

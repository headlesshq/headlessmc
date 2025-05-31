package io.github.headlesshq.headlessmc.version.parser;

import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.version.Features;
import io.github.headlesshq.headlessmc.version.Rule;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@EqualsAndHashCode
@RequiredArgsConstructor
final class ParsedRule implements Rule {
    @EqualsAndHashCode.Exclude
    private final Rule rule;
    @EqualsAndHashCode.Include
    private final String string;

    @Override
    public Action apply(OS os, Features features) {
        return rule.apply(os, features);
    }

    @Override
    public String toString() {
        return string;
    }

}

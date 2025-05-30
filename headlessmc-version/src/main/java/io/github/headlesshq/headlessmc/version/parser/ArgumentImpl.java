package io.github.headlesshq.headlessmc.version.parser;

import io.github.headlesshq.headlessmc.version.Argument;
import io.github.headlesshq.headlessmc.version.Rule;
import lombok.Data;

@Data
final class ArgumentImpl implements Argument {
    private final String value;
    private final Rule rule;

}

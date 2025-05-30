package io.github.headlesshq.headlessmc.version.parser;

import io.github.headlesshq.headlessmc.version.Arguments;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = {@Inject})
final class ArgumentParser {
    private final RuleParser ruleParser;

    public Arguments parse() {
        return null;
    }

}

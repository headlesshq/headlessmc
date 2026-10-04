package io.github.headlesshq.headlessmc.launcher.args;

import io.github.headlesshq.headlessmc.version.Version;
import org.jspecify.annotations.Nullable;

import java.util.List;

record McArgument(List<String> value) implements Version.Argument {
    McArgument(String value) {
        this(List.of(value));
    }

    @Override
    public @Nullable List<Version.Rule> getRules() {
        return null;
    }

}

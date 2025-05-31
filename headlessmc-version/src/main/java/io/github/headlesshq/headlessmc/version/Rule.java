package io.github.headlesshq.headlessmc.version;

import io.github.headlesshq.headlessmc.os.OS;

import java.util.function.BiFunction;

@FunctionalInterface
public interface Rule extends BiFunction<OS, Features, Rule.Action> {
    Rule ALLOW = (os, features) -> Action.ALLOW;
    Rule DISALLOW = (os, features) -> Action.DISALLOW;
    Rule UNDECIDED = (os, features) -> Action.UNDECIDED;

    default boolean allow(OS os, Features features) {
        return apply(os, features) == Action.ALLOW;
    }

    enum Action {
        ALLOW,
        DISALLOW,
        UNDECIDED
    }

}

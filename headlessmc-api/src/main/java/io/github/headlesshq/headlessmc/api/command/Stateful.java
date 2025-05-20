package io.github.headlesshq.headlessmc.api.command;

import lombok.Data;

public interface Stateful<S> {
    void setState(S state);

    S getState();

    S createState();

    Key<S> getStateKey();

    @Data
    class Key<S> {
        private final String name;
        private final Class<S> type;
    }

}

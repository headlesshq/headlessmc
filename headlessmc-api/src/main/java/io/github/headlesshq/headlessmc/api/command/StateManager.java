package io.github.headlesshq.headlessmc.api.command;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StateManager {
    private final Map<Stateful.Key<?>, Object> states = new ConcurrentHashMap<>();

    public <S> S getState(Stateful<S> holder) {
        S state = getState(holder.getStateKey());
        if (state == null) {
            state = holder.createState();
        }

        return state;
    }

    public <S> @Nullable S getState(Stateful.Key<S> key) {
        return key.getType().cast(states.get(key));
    }

    public <S> void setState(Stateful.Key<S> key, @Nullable S state) {
        if (state == null) {
            states.remove(key);
        } else {
            states.put(key, state);
        }
    }

}

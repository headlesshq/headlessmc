package io.github.headlesshq.headlessmc.api.command;

public interface Command<T> {
    void setContext(T ctx);

    T getContext();

    Class<T> getContextType();

}

package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.HeadlessMc;

public abstract class AbstractCommand<T extends HeadlessMc> implements Command<T> {
    protected T ctx;

    @Override
    public abstract Class<T> getContextType();

    @Override
    public void setContext(T ctx) {
        this.ctx = ctx;
    }

    @Override
    public T getContext() {
        return ctx;
    }

}

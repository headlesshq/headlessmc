package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.HeadlessMc;

public class CommandImpl extends AbstractCommand<HeadlessMc> {
    @Override
    public Class<HeadlessMc> getContextType() {
        return HeadlessMc.class;
    }

}

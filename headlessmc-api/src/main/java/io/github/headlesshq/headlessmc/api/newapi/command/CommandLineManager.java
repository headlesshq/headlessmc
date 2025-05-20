package io.github.headlesshq.headlessmc.api.newapi.command;

import org.jetbrains.annotations.Nullable;

public interface CommandLineManager {
    PicocliCommandContext getContext();

    @Nullable CommandContext getInteractiveContext();

    void setInteractiveContext(@Nullable CommandContext context);

    boolean isHidingPasswordsSupported();

    boolean isHidingPasswords();

    void setHidingPasswords(boolean hidingPasswords);

}

package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.HeadlessMc;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import picocli.CommandLine.Command;

@Command(
    name = "password",
    helpCommand = true,
    description = "Toggles password mode, i.e. if command input is hidden."
)
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordCommand implements Runnable {
    private HeadlessMc ctx;

    @Override
    public void run() {
        if (!ctx.getCommandLine().isHidingPasswordsSupported()) {
            throw new CommandException("Hiding inputs is not supported.");
        }

        ctx.getCommandLine().setHidingPasswords(!ctx.getCommandLine().isHidingPasswords());
        ctx.log((ctx.getCommandLine().isHidingPasswords() ? "En" : "Dis") + "abled password mode.");
    }

}

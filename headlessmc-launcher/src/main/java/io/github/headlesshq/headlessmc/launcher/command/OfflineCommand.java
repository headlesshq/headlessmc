package io.github.headlesshq.headlessmc.launcher.command;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

@RequiredArgsConstructor(onConstructor_ = {@Inject})
@CommandLine.Command(name = "offline", description = "Toggles offline mode.")
public class OfflineCommand {
    private final Launcher launcher;

    // TODO
    @Override
    public void execute(String line, String... args) throws CommandException {
        boolean value = !ctx.getAccountManager().getOfflineChecker().isOffline();
        if (args.length > 1) {
            value = Boolean.parseBoolean(args[1]);
        }

        ctx.getAccountManager().getOfflineChecker().setOffline(value);
        ctx.log("You are now " + (value ? "offline." : "online."));
    }

}

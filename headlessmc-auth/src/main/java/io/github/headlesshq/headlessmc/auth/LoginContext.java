package io.github.headlesshq.headlessmc.auth;

import io.github.headlesshq.headlessmc.api.Application;
import io.github.headlesshq.headlessmc.api.command.CommandContext;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.Suggestion;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class LoginContext implements CommandContext {
    protected final Application app;
    protected final @Nullable CommandContext commandContext;
    protected final String helpMessage;

    @Override
    public void execute(String message) {
        String lower = message.toLowerCase(Locale.ENGLISH);
        if (lower.equalsIgnoreCase("abort")) {
            app.log("Aborting login process.");
            returnToPreviousContext();
        } else if (lower.equalsIgnoreCase("help")) {
            app.log(helpMessage);
        } else {
            onCommand(message);
        }
    }

    @Override
    public void executeArgs(String... args) throws CommandException {
        onCommand(args[0]);
    }

    @Override
    public List<Suggestion> getSuggestions(int argIndex, int positionInArg, int cursor, String... args) {
        return new ArrayList<>();
    }

    protected void onCommand(String message) {
        // to be implemented by sub classes
    }

    protected void returnToPreviousContext() {
        app.getCommandLine().setInteractiveContext(commandContext);
    }

}

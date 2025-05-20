package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.command.picoli.CommandLineParser;
import io.github.headlesshq.headlessmc.api.command.picoli.CommandLineFactory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import picocli.AutoComplete;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link CommandContext}.
 */
@Getter
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "RedundantSuppression"}) // delegate
public class CommandContextImpl implements CommandContext {
    private final CommandLine picocli;

    public CommandContextImpl(HeadlessMc ctx, Object command) {
        this(new CommandLineFactory<>(ctx, command).create());
    }

    @Override
    public void execute(String command) {
        try {
            CommandLineParser parser = new CommandLineParser();
            String[] args = parser.parse(command);
            int result = picocli.execute(args);
        } catch (CommandException e) {
            e.printStackTrace();
        }

        //if (log.getConfig().get(HmcProperties.EXIT_ON_FAILED_COMMAND, false)) {
        //    log.getExitManager().exit(-1);
        //}
    }

    @Override
    public List<Map.Entry<String, @Nullable String>> getCompletions(String line) {
        // return AutoComplete.complete(commandLine, )
        return new ArrayList<>(); // TODO
    }

    @Override
    public <H extends HeadlessMc, C extends Command<H>> C getCommand(H headlessMc, Class<C> commandClass) {
        CommandLine commandLine = new CommandLineFactory<>(headlessMc, commandClass).create();
        try {
            return commandLine.getFactory().create(commandClass);
        } catch (Exception e) {
            throw new CommandException(e);
        }
    }

}

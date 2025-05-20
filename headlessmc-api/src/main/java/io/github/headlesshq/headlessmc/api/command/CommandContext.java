package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.HeadlessMc;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.util.List;
import java.util.Map;

public interface CommandContext {
    CommandLine getPicocli();

    void execute(String command);

    List<Map.Entry<String, @Nullable String>> getCompletions(String line);

    <H extends HeadlessMc, C extends Command<H>> C getCommand(H headlessMc, Class<C> commandClass) throws Exception;

}
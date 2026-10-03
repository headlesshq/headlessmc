package io.github.headlesshq.headlessmc.commands;

import io.github.headlesshq.headlessmc.HeadlessMc;
import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.auth.AuthService;
import io.github.headlesshq.headlessmc.commands.auth.AccountCommand;
import io.github.headlesshq.headlessmc.commands.config.ConfigCommand;
import io.github.headlesshq.headlessmc.commands.java.JavaCommand;
import io.github.headlesshq.headlessmc.commands.launcher.LaunchCommand;
import io.github.headlesshq.headlessmc.commands.mod.ModCommand;
import io.github.headlesshq.headlessmc.commands.profile.ProfileCommand;
import io.github.headlesshq.headlessmc.commands.server.ServerCommand;
import io.github.headlesshq.headlessmc.commands.version.VersionCommand;
import io.github.headlesshq.headlessmc.console.*;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.cache.ConsoleCachingService;
import io.github.headlesshq.headlessmc.console.completions.PicocliCompletions;
import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@Slf4j
@Getter
@Setter
@Dependent
@Named("headlessmc")
@CommandLine.Command(
    name = "headlessmc",
    description = "test",
    version = HeadlessMc.VERSION,
    mixinStandardHelpOptions = true,
    subcommands = {
        ConfigCommand.class,
        AccountCommand.class,
        JavaCommand.class,
        LaunchCommand.class,
        VersionCommand.class,
        ProfileCommand.class,
        ServerCommand.class,
        ModCommand.class,
        CommandLine.HelpCommand.class,
        DebugCommand.class
    }
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class HeadlessMcCommand implements Callable<Integer> {
    private final Provider<CommandLine> commandLineProvider;
    private final ConsoleCachingService cachingService;
    private final ArgSplitter argSplitter;
    private final Console console;

    @Override
    public Integer call() {
        CachedConsole console = cachingService.cache(this.console);
        try {
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new UncheckedInterruptedException();
                }

                try {
                    if (console == null) {
                        console = cachingService.cache(this.console);
                    }

                    String line = readLine(console.get());
                    String[] args = argSplitter.split(line);
                    CommandLine commandLine = commandLine();
                    console.get()
                        .extensions()
                        .ifPresent(ex -> commandLine.setUsageHelpWidth(Math.max(55, ex.getWidth())));
                    // TODO: we only close the Jline Terminal all the time because we are scared of spawning a
                    //  process that could use jline and because ctongfei/progressbar tries to open its own
                    //  Terminal AGHHHHHH FIX this could be so much simpler...
                    // Because we close the Jline Terminal on every console call
                    //  performance is a bit decreased, there is a gap after every command executed
                    //  to reopen the console.
                    //  cache Console and check if the command found implements a certain interface
                    //  CachedConsole.Enabled, if it does, don't close the console because we only need
                    //  to close the Jline Console on like LaunchCommand, because the child process
                    //  could open an own Jline console.
                    //  (or the config command may change jline configuration, so we want to reopen the console)
                    //  See JlineConsole.CachedConsole for a start
                    Object command = getCommand(commandLine, args);
                    if (!(command instanceof CachedConsole.Enabled)
                        && !(command instanceof CommandLine.HelpCommand)) {
                        console.close();
                        console = null;
                    }

                    int result = commandLine.execute(args);
                    if (command instanceof FinalCommand) {
                        return result;
                    }
                } catch (ConsoleException.Interrupted e) {
                    break;
                } catch (CommandLine.PicocliException e) {
                    log.error("", e);
                }
            }
        } finally {
            if (console != null) {
                console.close();
            }
        }

        return 0;
    }

    private @Nullable Object getCommand(CommandLine commandLine, String[] args) {
        CommandLine.ParseResult parseResult = commandLine.parseArgs(args);
        if (parseResult == null) {
            return null;
        }

        Object result = parseResult.commandSpec().userObject();
        while (parseResult != null) {
            result = parseResult.commandSpec().userObject();
            parseResult = parseResult.subcommand();
        }

        return result;
    }

    private CommandLine commandLine() {
        CommandLine commandLine = commandLineProvider.get();
        commandLine.addSubcommand(ExitCommand.class);
        return commandLine;
    }

    private String readLine(Console console) {
        return console.extensions()
            .map(extensions -> extensions.read("> ", new PicocliCompletions(this::commandLine)))
            .orElseGet(console::read);
    }

}

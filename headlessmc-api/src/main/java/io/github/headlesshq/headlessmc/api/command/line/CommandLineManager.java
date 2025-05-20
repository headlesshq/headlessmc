package io.github.headlesshq.headlessmc.api.command.line;

import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.HeadlessMcApi;
import io.github.headlesshq.headlessmc.api.command.*;
import io.github.headlesshq.headlessmc.api.process.InAndOutProvider;
import lombok.*;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.io.IOError;
import java.io.IOException;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Everything related to reading commands from the command line.
 */
@Getter
@Setter
@CustomLog
public class CommandLineManager implements PasswordAware, QuickExitCli, HasCommandContext, CommandLineReader {
    private final InAndOutProvider inAndOutProvider;
    private final PasswordAware passwordContext;
    /**
     * Provides the {@link CommandLineReader}.
     */
    private volatile Supplier<CommandLineReader> commandLineProvider;
    /**
     * The initial CommandContext, so that we do not lose it...
     */
    private volatile CommandContext baseContext;
    /**
     * The currently active CommandContext which executes commands written on the CommandLine managed by this CommandLineManager.
     */
    private volatile CommandContext commandContext;
    /**
     * Accesses the raw line read from the command line.
     * Is generally only meant for testing, should by default always call execute on {@link #getCommandContext()}.
     */
    private volatile Consumer<String> commandConsumer = line -> getCommandContext().execute(line);
    /**
     * The current {@link CommandLineReader} for reading the command line.
     * Might be {@code null}.
     */
    private volatile @Nullable CommandLineReader commandLineReader;

    private volatile boolean quickExitCli;
    private volatile boolean waitingForInput;
    private volatile boolean listening;
    private volatile boolean takenOverByOtherClassloader;

    /**
     * Constructs a new CommandLine.
     */
    public CommandLineManager() {
        this(new InAndOutProvider(), new PasswordAwareImpl());
    }

    /**
     * Constructs a new CommandLine for the given arguments.
     * @param inAndOutProvider the InAndOutProvider providing the Streams this CommandLine will listen on.
     */
    public CommandLineManager(InAndOutProvider inAndOutProvider, PasswordAware passwordContext) {
        this.inAndOutProvider = inAndOutProvider;
        this.passwordContext = passwordContext;
        this.baseContext = new CommandContextImpl(new CommandLine(new EmptyCommand()));
        this.commandContext = baseContext;
        this.commandLineProvider = new DefaultCommandLineProvider(inAndOutProvider);
        setHidingPasswordsSupported(inAndOutProvider.getConsole().get() != null);
    }

    @Override
    public void read(HeadlessMc hmc) throws IOError {
        if (this.commandLineReader != null) {
            log.warn("Listen called, but a CommandLineListener already exists!");
        }

        CommandLineReader commandLineReader = commandLineProvider.get();
        this.commandLineReader = commandLineReader;
        listening = true;
        commandLineReader.read(hmc);
    }

    @Override
    public void readAsync(HeadlessMc hmc, ThreadFactory factory) {
        if (this.commandLineReader != null) {
            log.warn("Listen called, but a CommandLineListener already exists!");
        }

        CommandLineReader commandLineReader = commandLineProvider.get();
        this.commandLineReader = commandLineReader;
        listening = true;
        commandLineReader.readAsync(hmc, factory);
    }

    @Override
    public void open(HeadlessMc hmc) throws IOException {
        CommandLineReader commandLineReader = getCommandLineReader();
        if (commandLineReader != null) {
            commandLineReader.read(hmc);
            listening = true;
        }
    }

    @Override
    public void close() throws IOException {
        CommandLineReader commandLineReader = getCommandLineReader();
        if (commandLineReader != null) {
            listening = false;
            commandLineReader.close();
        }
    }

    /**
     * Sets both {@link #baseContext} and {@link #commandContext} to the given context.
     * NOT Thread-Safe.
     *
     * @param context the new base- and command context.
     */
    public void setAllContexts(CommandContext context) {
        setBaseContext(context);
        setCommandContext(context);
    }

    @Override
    public boolean isHidingPasswords() {
        return passwordContext.isHidingPasswords();
    }

    @Override
    public void setHidingPasswords(boolean hidingPasswords) {
        passwordContext.setHidingPasswords(hidingPasswords);
    }

    @Override
    public boolean isHidingPasswordsSupported() {
        return passwordContext.isHidingPasswordsSupported();
    }

    @Override
    public void setHidingPasswordsSupported(boolean hidingPasswordsSupported) {
        passwordContext.setHidingPasswordsSupported(hidingPasswordsSupported);
    }

    @Override
    public Progressbar displayProgressBar(Progressbar.Configuration configuration) {
        CommandLineReader commandLineReader = this.commandLineReader;
        if (commandLineReader != null) {
            return commandLineReader.displayProgressBar(configuration);
        }

        return Progressbar.dummy();
    }

    @CommandLine.Command(
        name = "headlessmc",
        version = HeadlessMcApi.NAME_VERSION,
        mixinStandardHelpOptions = true,
        description = "Displays Memory stats."
    )
    @RequiredArgsConstructor
    private static final class EmptyCommand implements Runnable {
        @Override
        public void run() {

        }
    }

}

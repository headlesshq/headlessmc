package io.github.headlesshq.headlessmc.jline;

import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.command.line.CommandLineManager;
import io.github.headlesshq.headlessmc.api.command.line.CommandLineReader;
import io.github.headlesshq.headlessmc.api.command.line.Progressbar;
import io.github.headlesshq.headlessmc.api.config.Property;
import io.github.headlesshq.headlessmc.api.process.InAndOutProvider;
import org.jetbrains.annotations.Nullable;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOError;
import java.io.IOException;
import java.util.function.BiConsumer;

/**
 * An implementation of a {@link CommandLineReader} that reads commands from a JLine {@link Terminal} with a {@link LineReader}.
 */
@Getter
@CustomLog
public class JLineCommandLineReader implements CommandLineReader {
    private final JlineProgressbarProvider progressbarProvider = new JlineProgressbarProvider();

    /**
     * The prefix to display when reading from the command line.
     */
    @Setter
    protected volatile String readPrefix;

    /**
     * The LineReader we are currently reading from.
     * Might be {@code null}.
     */
    protected volatile @Nullable LineReader lineReader;
    /**
     * The Terminal that is currently in use.
     * Might be {@code null}.
     */
    protected volatile @Nullable Terminal terminal;
    /**
     * If the {@link #terminal} in use is a dumb terminal.
     * @see TerminalBuilder#dumb(boolean)
     */
    protected volatile boolean dumb;

    protected volatile boolean enableProgressbar = Boolean.parseBoolean(System.getProperty(JLineProperties.ENABLE_PROGRESS_BAR.getName(), "true"));

    @Override
    public void read(HeadlessMc hmc) throws IOError {
        CommandLineManager commandLine = hmc.getCommandLine();
        long nanos = System.nanoTime();
        try {
            open(hmc);
            nanos = System.nanoTime() - nanos;
            log.debug("JLine terminal took " + (nanos / 1_000_000.0) + "ms to get ready.");

            String line;
            while (true) {
                LineReader currentLineReader = lineReader;
                if (currentLineReader == null) {
                    break;
                }

                try {
                    line = commandLine.isHidingPasswords() ? currentLineReader.readLine(readPrefix, '*') : currentLineReader.readLine(readPrefix);
                } catch (EndOfFileException ignored) {
                    // Continue reading after EOT
                    continue;
                }

                if (line == null) {
                    break;
                }

                line = line.trim();
                commandLine.getCommandConsumer().accept(line);
                if (commandLine.isQuickExitCli()) {
                    if (!commandLine.isWaitingForInput()) {
                        break;
                    }

                    log.debug("Waiting for more input...");
                }
            }
        } catch (UserInterruptException | IOException e) {
            // TODO: on UserInterruptException kill Mc process?
            throw new IOError(e);
        } finally {
            try {
                close();
            } catch (IOException e) {
                log.error("Failed to close Terminal", e);
            }
        }
    }

    @Override
    public synchronized void open(HeadlessMc hmc) throws IOException {
        enableProgressbar = hmc.getConfig().get(JLineProperties.ENABLE_PROGRESS_BAR, true);
        progressbarProvider.setProgressBarStyle(hmc.getConfig().get(JLineProperties.PROGRESS_BAR_STYLE, null));
        if (hmc.getConfig().get(JLineProperties.PREVENT_DEPRECATION_WARNING, true)) {
            System.setProperty("org.args.terminal.disableDeprecatedProviderWarning", "true");
        }

        CommandLineManager commandLine = hmc.getCommandLine();
        dumb = !hmc.getConfig().get(JLineProperties.FORCE_NOT_DUMB, false)
            && (hmc.getConfig().get(JLineProperties.DUMB, false)
            || System.console() == null && hmc.getConfig().get(JLineProperties.DUMB_WHEN_NO_CONSOLE, true)
            || System.getProperty("java.class.path").contains("idea_rt.jar"));

        String providers = hmc.getConfig().get(JLineProperties.PROVIDERS, "jni");
        InAndOutProvider io = commandLine.getInAndOutProvider();

        Terminal currentTerminal = buildTerminal(hmc, dumb, providers, io);
        log.info("JLine Terminal type: " + currentTerminal.getType() + ", name: " + currentTerminal.getName() + " (" + currentTerminal + ")");
        LineReader reader = buildLineReader(currentTerminal, hmc);
        reader.setOpt(LineReader.Option.DISABLE_EVENT_EXPANSION);
        if (!hmc.getConfig().get(JLineProperties.BRACKETED_PASTE, true)) {
            reader.unsetOpt(LineReader.Option.BRACKETED_PASTE);
        }

        reader.unsetOpt(LineReader.Option.INSERT_TAB);
        this.readPrefix = hmc.getConfig().get(JLineProperties.READ_PREFIX, null);
        this.terminal = currentTerminal;
        this.lineReader = reader;
    }

    @Override
    public synchronized void close() throws IOException {
        try {
            Terminal currentTerminal = terminal;
            if (currentTerminal != null) {
                log.debug("Closing Terminal!");
                currentTerminal.close();
            }
        } finally {
            terminal = null;
            lineReader = null;
        }
    }

    @Override
    public Progressbar displayProgressBar(Progressbar.Configuration configuration) {
        Terminal currentTerminal = terminal;
        if (currentTerminal == null || !enableProgressbar) {
            return Progressbar.dummy();
        }

        return progressbarProvider.displayProgressBar(configuration);
    }

    protected Terminal buildTerminal(HeadlessMc hmc, boolean dumb, String providers, InAndOutProvider io) throws IOException {
        return buildTerminalBuilder(hmc, dumb, providers, io).build();
    }

    protected LineReader buildLineReader(Terminal terminal, HeadlessMc hmc) {
        return LineReaderBuilder.builder().appName("HeadlessMC").terminal(terminal).completer(new CommandCompleter(hmc)).build();
    }

    protected TerminalBuilder buildTerminalBuilder(HeadlessMc hmc, boolean dumb, String providers, InAndOutProvider io) {
        // terribly complicated TerminalBuilder because on Windows JLine cannot be trusted to find the correct provider?!?!?!?!
        // Honestly this is kinda weird
        TerminalBuilder terminalBuilder = TerminalBuilder
                .builder()
                .streams(hmc.getConfig().get(JLineProperties.JLINE_IN, false) ? io.getIn().get() : null,
                        hmc.getConfig().get(JLineProperties.JLINE_OUT, false) ? io.getOut().get() : null)
                .dumb(dumb)
                .type(hmc.getConfig().get(JLineProperties.TYPE, null));

        configureNullable(terminalBuilder, JLineProperties.EXEC, hmc, TerminalBuilder::exec, false);
        configureNullable(terminalBuilder, JLineProperties.JNI, hmc, TerminalBuilder::jni, true);
        configureNullable(terminalBuilder, JLineProperties.JANSI, hmc, TerminalBuilder::jansi, false);
        configureNullable(terminalBuilder, JLineProperties.JNA, hmc, TerminalBuilder::jna, true);
        configureNullable(terminalBuilder, JLineProperties.SYSTEM, hmc, TerminalBuilder::system, null);

        try {
            terminalBuilder.ffm(hmc.getConfig().get(JLineProperties.FFM, false));
            terminalBuilder.providers(providers);
        } catch (NoSuchMethodError ignored) { // e.g. 1.12.2 ships an older version of JLine which does not have this
            log.debug("Running an older version of JLine, FFM and/or providers not supported.");
        }

        return terminalBuilder;
    }

    protected void configureNullable(TerminalBuilder builder,
                                     Property<Boolean> property,
                                     HeadlessMc hmc,
                                     BiConsumer<TerminalBuilder, Boolean> action,
                                     @Nullable Boolean def) {
        Boolean value = hmc.getConfig().get(property, def);
        if (value != null) {
            action.accept(builder, value);
        }
    }

}

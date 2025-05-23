package io.github.headlesshq.headlessmc.jline;

import io.github.headlesshq.headlessmc.api.Application;
import io.github.headlesshq.headlessmc.api.command.*;
import io.github.headlesshq.headlessmc.api.logging.StdIO;
import io.github.headlesshq.headlessmc.api.settings.SettingKey;
import lombok.CustomLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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
@RequiredArgsConstructor
public class JLineCommandLineReader implements CommandLineReader, HidesPasswords {
    private final JlineProgressbarProvider progressbarProvider = new JlineProgressbarProvider();

    private final JLineSettings settings;

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

    @Setter
    protected volatile boolean hidingPasswords;
    protected volatile boolean enableProgressbar;

    @Override
    public void read(Application hmc) throws IOError {
        CommandLineManager commandLine = hmc.getCommandLine();
        CommandContext context = commandLine.getInteractiveContext();
        if (context == null) {
            return;
        }

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
                    line = isHidingPasswords()
                            ? currentLineReader.readLine(readPrefix, '*')
                            : currentLineReader.readLine(readPrefix);
                } catch (EndOfFileException ignored) {
                    // Continue reading after EOT
                    continue;
                }

                if (line == null) {
                    break;
                }

                line = line.trim();
                context = commandLine.getInteractiveContext();
                if (context == null) {
                    return;
                }

                context.execute(line);
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
    public synchronized void open(Application hmc) throws IOException {
        enableProgressbar = hmc.getConfig().get(settings.progressBar);
        progressbarProvider.setProgressBarStyle(hmc.getConfig().get(settings.progressBarStyle));
        if (hmc.getConfig().get(settings.noDeprecationWarning)) {
            System.setProperty("org.args.terminal.disableDeprecatedProviderWarning", "true");
        }

        CommandLineManager commandLine = hmc.getCommandLine();
        dumb = !hmc.getConfig().get(settings.forceNotDumb)
            && (hmc.getConfig().get(settings.dumb)
            || System.console() == null && hmc.getConfig().get(settings.dumbWhenNoConsole)
            || System.getProperty("java.class.path").contains("idea_rt.jar"));

        String providers = hmc.getConfig().get(settings.providers);
        StdIO io = commandLine.getStdIO();

        Terminal currentTerminal = buildTerminal(hmc, dumb, providers, io);
        log.info("JLine Terminal type: " + currentTerminal.getType() + ", name: " + currentTerminal.getName() + " (" + currentTerminal + ")");
        LineReader reader = buildLineReader(currentTerminal, hmc);
        reader.setOpt(LineReader.Option.DISABLE_EVENT_EXPANSION);
        if (!hmc.getConfig().get(settings.bracketedPaste)) {
            reader.unsetOpt(LineReader.Option.BRACKETED_PASTE);
        }

        reader.unsetOpt(LineReader.Option.INSERT_TAB);
        this.readPrefix = hmc.getConfig().get(settings.readPrefix);
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

    protected Terminal buildTerminal(Application hmc, boolean dumb, String providers, StdIO io) throws IOException {
        return buildTerminalBuilder(hmc, dumb, providers, io).build();
    }

    protected LineReader buildLineReader(Terminal terminal, Application hmc) {
        return LineReaderBuilder.builder().appName("HeadlessMc").terminal(terminal).completer(new CommandCompleter(hmc)).build();
    }

    protected TerminalBuilder buildTerminalBuilder(Application hmc, boolean dumb, String providers, StdIO io) {
        // terribly complicated TerminalBuilder because on Windows JLine cannot be trusted to find the correct provider?!?!?!?!
        // Honestly this is kinda weird
        TerminalBuilder terminalBuilder = TerminalBuilder
                .builder()
                .streams(hmc.getConfig().get(settings.jlineIn) ? io.getIn().get() : null,
                        hmc.getConfig().get(settings.jlineOut) ? io.getOut().get() : null)
                .dumb(dumb)
                .type(hmc.getConfig().get(settings.type));

        configureNullable(terminalBuilder, settings.exec, hmc, TerminalBuilder::exec);
        configureNullable(terminalBuilder, settings.jni, hmc, TerminalBuilder::jni);
        configureNullable(terminalBuilder, settings.jansi, hmc, TerminalBuilder::jansi);
        configureNullable(terminalBuilder, settings.jna, hmc, TerminalBuilder::jna);
        configureNullable(terminalBuilder, settings.system, hmc, TerminalBuilder::system);

        try {
            terminalBuilder.ffm(hmc.getConfig().get(settings.ffm));
            terminalBuilder.providers(providers);
        } catch (NoSuchMethodError ignored) { // e.g. 1.12.2 ships an older version of JLine which does not have this
            log.debug("Running an older version of JLine, FFM and/or providers not supported.");
        }

        return terminalBuilder;
    }

    protected void configureNullable(TerminalBuilder builder,
                                     SettingKey<Boolean> property,
                                     Application hmc,
                                     BiConsumer<TerminalBuilder, Boolean> action) {
        Boolean value = hmc.getConfig().get(property);
        if (value != null) {
            action.accept(builder, value);
        }
    }

}

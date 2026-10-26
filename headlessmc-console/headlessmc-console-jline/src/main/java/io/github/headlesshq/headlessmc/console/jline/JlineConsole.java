package io.github.headlesshq.headlessmc.console.jline;

import io.github.headlesshq.headlessmc.HeadlessMc;
import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.console.*;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
class JlineConsole implements Console, ConsoleExtensions {
    private final Holder<JlineConfig> config;
    /**
     * Guards {@link #activeReader}. The read lock is held while printing through the active reader,
     * so that the reader cannot finish reading and its terminal cannot be closed in the meantime.
     * Printing only needs the read lock, so a completer that writes while the reader is reading
     * does not deadlock with another thread waiting in {@link LineReader#printAbove(String)}.
     */
    final ReadWriteLock readerLock = new ReentrantReadWriteLock();
    /**
     * The reader currently waiting for input, while it is reading {@link #wrap(Function)} is locked,
     * so messages from other threads have to be printed above its prompt through this reader directly.
     */
    private @Nullable LineReader activeReader;

    @Override
    public String read() {
        return wrap(state -> reading(state.reader(), LineReader::readLine));
    }

    @Override
    public Password readPassword() {
        return wrap(state -> Password.of(reading(state.reader(), reader -> reader.readLine(null, '*'))));
    }

    @Override
    public String read(String message) {
        return wrap(state -> reading(state.reader(), reader -> reader.readLine(message)));
    }

    @Override
    public Password readPassword(String message) {
        return wrap(state -> Password.of(reading(state.reader(), reader -> reader.readLine(message, '*'))));
    }

    @Override
    public void write(String message) {
        readerLock.readLock().lock();
        try {
            if (activeReader != null) {
                activeReader.printAbove(message);
                return;
            }
        } finally {
            readerLock.readLock().unlock();
        }

        wrap(state -> {
            state.reader().printAbove(message);
            return 0;
        });
    }

    @Override
    public void write(String message, String nextLine) {
        wrap(state -> {
            if (nextLine.contains("\n") || nextLine.contains("\n\033[m") || nextLine.contains("\n\033[0m")) {
                state.reader().printAbove(message);
            } else {
                state.terminal.writer().print(message + nextLine);
            }

            return 0;
        });
    }

    @Override
    public String edit(String initialString) {
        return wrap(state -> {
            // maybe we could also check the terminal capabilities for if this is supported?
            if (Terminal.TYPE_DUMB.equalsIgnoreCase(state.terminal.getType())
                || Terminal.TYPE_DUMB_COLOR.equalsIgnoreCase(state.terminal.getType())
            ) {
                return reading(state.reader(), reader -> reader.readLine("[" + initialString + "]:"));
            }

            return reading(state.reader(), reader -> reader.readLine(null, null, initialString));
        });
    }

    @Override
    public int getWidth() {
        return wrap(state -> Math.max(55, state.terminal.getColumns()));
    }

    @Override
    public Optional<ConsoleExtensions> extensions() {
        return Optional.of(this);
    }

    @Override
    public String read(String prompt, Completions completions) {
        return wrap(state -> {
            Parser parser = new DefaultParser();
            LineReaderBuilder readerBuilder = LineReaderBuilder.builder()
                .terminal(state.terminal)
                .appName(HeadlessMc.NAME)
                // it's possible that someone tries to access this Console from Completions
                // that is a problem as you are never allowed to call wrap in a wrap call
                // but why would anyone do that? Maybe to check terminal size?
                .completer(new CompleterImpl(completions))
                .parser(parser)
                .option(LineReader.Option.AUTO_LIST, true)
                .option(LineReader.Option.LIST_PACKED, true)
                .option(LineReader.Option.AUTO_MENU, true)
                .option(LineReader.Option.MENU_COMPLETE, true);

            // TODO: configure AUTO_MENU_LIST
            if (config.get().persistentCompletions()) {
                //readerBuilder.option(LineReader.Option.AUTO_MENU_LIST, true);
            }

            LineReader reader = readerBuilder.build();
            if (config.get().persistentCompletions()) {
                installPersistentCompletions(reader);
                reader.setVariable(LineReader.TOO_MANY_CANDIDATES, config.get().tooManyCandidates().orElse("partial"));
            }

            try {
                return reading(reader, lineReader -> lineReader.readLine(prompt, null, (MaskingCallback) null, null));
            } catch (UserInterruptException e) {
                throw new ConsoleException.Interrupted();
            }
        });
    }

    @Override
    public CachedConsole cache() {
        try {
            TerminalState state = new TerminalState(TerminalBuilder.builder().build());
            return new JlineCachedConsole(config, state);
        } catch (IOException e) {
            throw new ConsoleException(e);
        }
    }

    void test() {
        try {
            TerminalBuilder.builder().build().close();
        } catch (IOException e) {
            throw new ConsoleException("Failed to open Jline console", e);
        }
    }

    /**
     * Makes the given reader the {@link #activeReader} while it reads.
     * The terminal of the reader is only closed after this method returns,
     * so {@link #write(String)} never prints through a reader with a closed terminal.
     */
    private <T> T reading(LineReader reader, Function<LineReader, T> read) {
        setActiveReader(reader);
        try {
            return read.apply(reader);
        } finally {
            setActiveReader(null);
        }
    }

    private void setActiveReader(@Nullable LineReader reader) {
        readerLock.writeLock().lock();
        try {
            activeReader = reader;
        } finally {
            readerLock.writeLock().unlock();
        }
    }

    synchronized <T> T wrap(Function<TerminalState, T> action) {
        try (TerminalState state = new TerminalState(TerminalBuilder.builder().build())) {
            return action.apply(state);
        } catch (UserInterruptException e) {
            throw new UncheckedInterruptedException(e);
        } catch (EndOfFileException | IOException e) {
            throw new ConsoleException(e);
        }
    }

    private void installPersistentCompletions(LineReader reader) {
        installPersistentCompletions(reader, LineReader.EXPAND_OR_COMPLETE);
        installPersistentCompletions(reader, LineReader.MENU_COMPLETE);

        installPersistentCompletions(reader, LineReader.COMPLETE_WORD);

        installPersistentCompletions(reader, LineReader.SELF_INSERT);
        installPersistentCompletions(reader, LineReader.BACKWARD_DELETE_CHAR);
        installPersistentCompletions(reader, LineReader.CALLBACK_INIT);

        installPersistentCompletions(reader, LineReader.BACKWARD_DELETE_WORD);

        installPersistentCompletions(reader, LineReader.BACKWARD_KILL_WORD);
        installPersistentCompletions(reader, LineReader.BACKWARD_KILL_LINE);

        installPersistentCompletions(reader, LineReader.BACKWARD_CHAR);
        installPersistentCompletions(reader, LineReader.BACKWARD_WORD);
    }

    private void installPersistentCompletions(LineReader reader, String widgetName) {
        Widget widget = reader.getWidgets().get(widgetName);
        Widget completionWidget = () -> {
            boolean result = widget == null || widget.apply();
            reader.callWidget(LineReader.LIST_CHOICES);
            return result;
        };

        reader.getWidgets().put(widgetName, completionWidget);
    }

    record TerminalState(Terminal terminal) implements Closeable {
        LineReader reader() {
            return LineReaderBuilder.builder().terminal(terminal).build();
        }

        @Override
        public void close() throws IOException {
            terminal.close();
        }
    }

    private static final class JlineCachedConsole extends JlineConsole implements CachedConsole {
        private final TerminalState state;

        public JlineCachedConsole(Holder<JlineConfig> config, TerminalState state) {
            super(config);
            this.state = state;
        }

        @Override
        synchronized <T> T wrap(Function<TerminalState, T> action) {
            try {
                return action.apply(state);
            } catch (UserInterruptException e) {
                throw new UncheckedInterruptedException(e);
            } catch (EndOfFileException e) {
                throw new ConsoleException(e);
            }
        }

        @Override
        public Console get() {
            return this;
        }

        @Override
        public void close() throws ConsoleException {
            try {
                readerLock.writeLock().lock();
                try {
                    state.close();
                } finally {
                    readerLock.writeLock().unlock();
                }
            } catch (IOException e) {
                throw new ConsoleException(e);
            }
        }
    }

}

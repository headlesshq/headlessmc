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
import org.jline.utils.InfoCmp;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
class JlineConsole implements Console, ConsoleExtensions {
    private static final String MODE_2027_ENABLE = "\033[?2027h";
    private static final String MODE_2027_DISABLE = "\033[?2027l";
    /**
     * How long {@link #terminalInfo()} reuses the last result, querying the terminal size can spawn a process.
     */
    private static final long TERMINAL_INFO_MAX_AGE_NANOS = TimeUnit.SECONDS.toNanos(1);

    private final Holder<JlineConfig> config;
    /**
     * Guards {@link #terminal} and {@link #leases}.
     * Held while opening, closing and querying a terminal, but never while reading,
     * so it can only block for a short time.
     */
    private final Lock terminalLock = new ReentrantLock();
    /**
     * The terminal that is currently open. Jline only allows one system terminal at a time,
     * so everything that needs a terminal shares this one, see {@link #openTerminal()}.
     */
    private @Nullable TerminalState terminal;
    /**
     * How many {@link TerminalLease}s for the {@link #terminal} have not been closed yet.
     */
    private int leases;
    /**
     * The system terminal stays the same, so its grapheme cluster support only has to be detected once.
     */
    private volatile @Nullable GraphemeClusterSupport detectedGraphemeClusterSupport;
    private volatile @Nullable TerminalInfo lastTerminalInfo;
    private volatile long lastTerminalInfoNanos;
    /**
     * Guards {@link #activeReader}. The read lock is held while printing through the active reader,
     * so that the reader cannot finish reading and its terminal cannot be closed in the meantime.
     * Printing only needs the read lock, so a completer that writes while the reader is reading
     * does not deadlock with another thread waiting in {@link LineReader#printAbove(String)}.
     */
    private final ReadWriteLock readerLock = new ReentrantReadWriteLock();
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
            if (isDumb(state.terminal)) {
                return reading(state.reader(), reader -> reader.readLine("[" + initialString + "]:"));
            }

            return reading(state.reader(), reader -> reader.readLine(null, null, initialString));
        });
    }

    @Override
    public int getWidth() {
        return wrap(state -> Math.max(55, state.terminal.getColumns()));
    }

    /**
     * Unlike the other methods, this does not go through {@link #wrap(Function)},
     * which is locked while another thread waits for input.
     * Instead, it queries the terminal that is already open, e.g. by a {@link CachedConsole} or a reader,
     * and only opens a terminal itself if there is none.
     * The result is reused for {@link #TERMINAL_INFO_MAX_AGE_NANOS}.
     */
    @Override
    public TerminalInfo terminalInfo() {
        TerminalInfo info = lastTerminalInfo;
        if (info != null && System.nanoTime() - lastTerminalInfoNanos < TERMINAL_INFO_MAX_AGE_NANOS) {
            return info;
        }

        try {
            if (!terminalLock.tryLock(100, TimeUnit.MILLISECONDS)) {
                return info == null ? TerminalInfo.UNKNOWN : info;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return info == null ? TerminalInfo.UNKNOWN : info;
        }

        try {
            info = queryTerminalInfo();
            if (info != null) {
                lastTerminalInfo = info;
                lastTerminalInfoNanos = System.nanoTime();
                return info;
            }

            info = lastTerminalInfo;
            return info == null ? TerminalInfo.UNKNOWN : info;
        } finally {
            terminalLock.unlock();
        }
    }

    private @Nullable TerminalInfo queryTerminalInfo() {
        TerminalState state = terminal;
        if (state != null) {
            try {
                return terminalInfo(state.terminal());
            } catch (RuntimeException e) {
                log.debug("Failed to query terminal {}", state.terminal(), e);
                return null;
            }
        }

        // no terminal is open, and none can be opened while we hold the terminalLock,
        // the grapheme cluster mode does not matter for the size, so we skip probing it.
        try (Terminal temporary = TerminalBuilder.builder().dumb(true).graphemeCluster(false).build()) {
            return terminalInfo(temporary);
        } catch (IOException | RuntimeException e) {
            log.debug("Failed to open terminal to query its size", e);
            return null;
        }
    }

    private static TerminalInfo terminalInfo(Terminal terminal) {
        return new TerminalInfo(
            terminal.getColumns(),
            terminal.getStringCapability(InfoCmp.Capability.cursor_up) != null
                && terminal.getStringCapability(InfoCmp.Capability.cursor_down) != null
        );
    }

    private static boolean isDumb(Terminal terminal) {
        return Terminal.TYPE_DUMB.equalsIgnoreCase(terminal.getType())
            || Terminal.TYPE_DUMB_COLOR.equalsIgnoreCase(terminal.getType());
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
            return new JlineCachedConsole(openTerminal());
        } catch (IOException e) {
            throw new ConsoleException(e);
        }
    }

    /**
     * Opens the system terminal, or shares it if it is already open, e.g. by a {@link CachedConsole}.
     * The terminal is closed once every returned lease has been closed,
     * so that processes we spawn in the meantime can use the terminal themselves.
     */
    TerminalLease openTerminal() throws IOException {
        terminalLock.lock();
        try {
            TerminalState state = terminal;
            if (state == null) {
                state = buildTerminal();
                terminal = state;
            }

            leases++;
            return new TerminalLease(state);
        } finally {
            terminalLock.unlock();
        }
    }

    @Nullable TerminalState currentTerminal() {
        terminalLock.lock();
        try {
            return terminal;
        } finally {
            terminalLock.unlock();
        }
    }

    private void release() throws IOException {
        terminalLock.lock();
        try {
            if (--leases == 0) {
                TerminalState state = terminal;
                terminal = null;
                if (state != null) {
                    state.close();
                }
            }
        } finally {
            terminalLock.unlock();
        }
    }

    /**
     * Builds a new system terminal, with grapheme clusters configured by {@link JlineConfig#graphemeCluster()}.
     * Detecting grapheme cluster support makes Jline probe the terminal with escape sequences
     * and print test emojis to measure the cursor movement. That is visible and slow,
     * and we open a new terminal for every {@link #wrap(Function)} call, so the result is detected only once.
     */
    private TerminalState buildTerminal() throws IOException {
        TerminalBuilder builder = TerminalBuilder.builder();
        switch (config.get().graphemeCluster()) {
            case OFF -> builder.graphemeCluster(false);
            case ON -> builder.graphemeCluster(true);
            case AUTO -> {
                GraphemeClusterSupport support = detectedGraphemeClusterSupport;
                if (support == null) {
                    Terminal built = builder.build();
                    detectedGraphemeClusterSupport = GraphemeClusterSupport.of(built);
                    return new TerminalState(built, false);
                }

                return support.open(builder);
            }
        }

        return new TerminalState(builder.build(), false);
    }

    void test() {
        try {
            openTerminal().close();
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
        try (TerminalLease lease = openTerminal()) {
            return action.apply(lease.state());
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

    /**
     * @param mode2027 if we enabled the grapheme cluster mode 2027 of the terminal ourselves and have to disable it again.
     */
    record TerminalState(Terminal terminal, boolean mode2027) implements Closeable {
        LineReader reader() {
            return LineReaderBuilder.builder().terminal(terminal).build();
        }

        @Override
        public void close() throws IOException {
            if (mode2027) {
                terminal.writer().write(MODE_2027_DISABLE);
                terminal.writer().flush();
            }

            terminal.close();
        }
    }

    /**
     * A share of the {@link #terminal}, see {@link #openTerminal()}.
     */
    final class TerminalLease implements Closeable {
        private final TerminalState state;
        private final AtomicBoolean closed = new AtomicBoolean();

        private TerminalLease(TerminalState state) {
            this.state = state;
        }

        TerminalState state() {
            return state;
        }

        @Override
        public void close() throws IOException {
            if (closed.compareAndSet(false, true)) {
                release();
            }
        }
    }

    /**
     * Grapheme cluster support detected by Jline, so that terminals opened later do not have to probe again.
     */
    private enum GraphemeClusterSupport {
        NONE,
        /**
         * The terminal groups grapheme clusters by itself.
         * Jline also detects terminals which only group some emojis, e.g. only flags,
         * but that cannot be configured without probing, so these are treated as grouping all emojis.
         */
        NATIVE,
        /**
         * The terminal groups grapheme clusters once mode 2027 has been enabled.
         */
        MODE_2027;

        static GraphemeClusterSupport of(Terminal terminal) {
            if (!terminal.getGraphemeClusterMode()) {
                return NONE;
            }

            // the result of the mode probe is cached by the terminal, so this does not probe again
            return terminal.isModeSupported(Terminal.Mode.GRAPHEME_CLUSTER) ? MODE_2027 : NATIVE;
        }

        TerminalState open(TerminalBuilder builder) throws IOException {
            if (this == NONE) {
                return new TerminalState(builder.graphemeCluster(false).build(), false);
            }

            // forcing grapheme cluster mode skips the probe, but makes Jline assume native support
            Terminal terminal = builder.graphemeCluster(true).build();
            if (this == MODE_2027) {
                terminal.writer().write(MODE_2027_ENABLE);
                terminal.writer().flush();
            }

            return new TerminalState(terminal, this == MODE_2027);
        }
    }

    /**
     * Keeps the terminal open until closed, {@link #wrap(Function)} shares it in the meantime.
     * The terminal is only closed once all readers are done with it,
     * because every {@link #wrap(Function)} call holds its own lease.
     */
    private final class JlineCachedConsole implements CachedConsole {
        private final TerminalLease lease;

        private JlineCachedConsole(TerminalLease lease) {
            this.lease = lease;
        }

        @Override
        public Console get() {
            return JlineConsole.this;
        }

        @Override
        public void close() throws ConsoleException {
            try {
                lease.close();
            } catch (IOException e) {
                throw new ConsoleException(e);
            }
        }
    }

}

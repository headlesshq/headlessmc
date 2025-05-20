package io.github.headlesshq.headlessmc.jline;

import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.HeadlessMcImpl;
import io.github.headlesshq.headlessmc.api.command.line.CommandLineManager;
import io.github.headlesshq.headlessmc.api.config.ConfigImpl;
import io.github.headlesshq.headlessmc.api.exit.ExitManager;
import io.github.headlesshq.headlessmc.api.process.WritableInputStream;
import io.github.headlesshq.headlessmc.logging.LoggingService;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.terminal.impl.DumbTerminalProvider;
import org.jline.terminal.spi.SystemStream;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JLineCommandListenerTest {
    @Test
    @SuppressWarnings("deprecation")
    public void testJLineCommandListener() throws IOException {
        CommandLineManager commandLine = new CommandLineManager();
        WritableInputStream wis = new WritableInputStream();
        commandLine.getInAndOutProvider().setIn(() -> wis);
        commandLine.getInAndOutProvider().setOut(() -> System.out);
        commandLine.setQuickExitCli(true);
        commandLine.setWaitingForInput(false);
        System.setIn(wis);
        AtomicReference<String> readLine = new AtomicReference<>();
        commandLine.setCommandConsumer(line -> {
            readLine.set(line);
            try {
                ((JLineCommandLineReader) Objects.requireNonNull(commandLine.getCommandLineReader())).getTerminal().close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        System.setProperty(JLineProperties.DUMB.getName(), "false");
        System.setProperty(JLineProperties.FORCE_NOT_DUMB.getName(), "true");
        System.setProperty(JLineProperties.JNA.getName(), "false");
        System.setProperty(JLineProperties.JNI.getName(), "false");
        System.setProperty(JLineProperties.JLINE_IN.getName(), "true");
        System.setProperty(JLineProperties.JLINE_OUT.getName(), "true");
        commandLine.setCommandLineProvider(JLineCommandLineReader::new);

        TerminalBuilder.setTerminalOverride(new DumbTerminal(
                new DumbTerminalProvider(), SystemStream.Output, "dumb", "dumb",
                commandLine.getInAndOutProvider().getIn().get(), commandLine.getInAndOutProvider().getOut().get(),
                StandardCharsets.UTF_8, Terminal.SignalHandler.SIG_IGN));

        HeadlessMc hmc = new HeadlessMcImpl(ConfigImpl::empty, commandLine, new ExitManager(), new LoggingService());
        hmc.getLoggingService().setFileHandler(false);
        hmc.getLoggingService().init();
        hmc.getLoggingService().setLevel(Level.FINE);
        wis.getPrintStream().println("test");
        commandLine.read(hmc);
        assertEquals("test", readLine.get());
    }

}

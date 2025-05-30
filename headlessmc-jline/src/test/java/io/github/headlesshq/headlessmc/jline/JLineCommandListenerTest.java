package io.github.headlesshq.headlessmc.jline;

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class JLineCommandListenerTest {
    @Test
    @SuppressWarnings("deprecation")
    public void testJLineCommandListener() throws IOException {
        /* TODO: CommandLineManager commandLine = new CommandLineManager();
        WritableInputStream wis = new WritableInputStream();
        commandLine.getStdIO().setIn(() -> wis);
        commandLine.getStdIO().setOut(() -> System.out);
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
                commandLine.getStdIO().getIn().get(), commandLine.getStdIO().getOut().get(),
                StandardCharsets.UTF_8, Terminal.SignalHandler.SIG_IGN));

        HeadlessMc hmc = new HeadlessMcImpl(ConfigImpl::empty, commandLine, new ExitManager(), new LoggingService());
        hmc.getLoggingService().setFileHandler(false);
        hmc.getLoggingService().init();
        hmc.getLoggingService().setLevel(Level.FINE);
        wis.getPrintStream().println("test");
        commandLine.read(hmc);
        assertEquals("test", readLine.get());*/
    }

}

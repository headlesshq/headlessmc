package io.github.headlesshq.headlessmc.launcher;

import lombok.val;
import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.command.CommandLineReader;
import org.junit.jupiter.api.Test;

import java.io.IOError;
import java.util.concurrent.ThreadFactory;

import static io.github.headlesshq.headlessmc.launcher.QuickExitCliHandler.checkQuickExit;
import static org.junit.jupiter.api.Assertions.*;

public class QuickExitCliHandlerTest {
    @Test
    public void testQuickExitCliHandler() {
        val launcher = LauncherMock.INSTANCE;
        val in = new MockCommandLineReader();
        launcher.getCommandLine().setCommandLineProvider(() -> in);

        assertFalse(checkQuickExit(launcher, ""));
        assertFalse(checkQuickExit(launcher, "test"));
        assertFalse(checkQuickExit(launcher, "test", "test"));
        assertFalse(checkQuickExit(launcher, "--command"));
        assertFalse(checkQuickExit(launcher, "--command", ""));
        assertFalse(checkQuickExit(launcher, "--command", "cli"));

        assertTrue(checkQuickExit(launcher, "--version"));
        assertFalse(in.called);
        assertFalse(launcher.getCommandLine().isQuickExitCli());

        val ctx = new MockedCommandContext();
        val waitForInput = "waitForInput";
        ctx.setCallback(str -> {
            if (str.equals(waitForInput)) {
                launcher.getCommandLine().setWaitingForInput(true);
            }
        });
        launcher.getCommandLine().setCommandContext(ctx);

        assertTrue(checkQuickExit(launcher, "--command", "test"));
        assertTrue(launcher.getCommandLine().isQuickExitCli());
        assertEquals("test", ctx.checkAndReset());
        launcher.getCommandLine().setQuickExitCli(false);
        assertFalse(in.called);

        assertTrue(checkQuickExit(launcher, "--command", "waitForInput"));
        assertTrue(launcher.getCommandLine().isQuickExitCli());
        assertEquals("waitForInput", ctx.checkAndReset());
        assertTrue(launcher.getCommandLine().isWaitingForInput());
        assertTrue(in.called);
    }

    private static final class MockCommandLineReader implements CommandLineReader {
        public boolean called;

        @Override
        public void read(HeadlessMc hmc) throws IOError {
            called = true;
        }

        @Override
        public void readAsync(HeadlessMc hmc, ThreadFactory factory) {
            called = true;
        }
    }

}

package io.github.headlesshq.headlessmc.launcher.command.login;

import io.github.headlesshq.headlessmc.launcher.LauncherMock;
import io.github.headlesshq.headlessmc.launcher.command.LaunchContext;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PasswordContextTest {
    @Test
    public void testPasswordContext() {
        val launcher = LauncherMock.INSTANCE;
        val ctx = new LaunchContext(launcher);
        launcher.getCommandLine().setCommandContext(ctx);
        launcher.getCommandLine().setHidingPasswords(false);
        launcher.getCommandLine().setWaitingForInput(false);
        Assertions.assertFalse(launcher.getCommandLine().isWaitingForInput());
        Assertions.assertEquals(ctx, launcher.getCommandLine().getCommandContext());
        Assertions.assertFalse(launcher.getCommandLine().isHidingPasswords());

        /*launcher.getCommandLineManager().getCommandContext().execute("login test");
        Assertions.assertInstanceOf(PasswordContext.class,
                                    launcher.getCommandLineManager().getCommandContext());
        Assertions.assertTrue(launcher.isHidingPasswords());
        Assertions.assertTrue(launcher.isWaitingForInput());

        launcher.getCommandLineManager().getCommandContext().execute("abort");
        Assertions.assertFalse(launcher.isHidingPasswords());
        Assertions.assertEquals(ctx, launcher.getCommandLineManager().getCommandContext());
        Assertions.assertFalse(launcher.isWaitingForInput());*/
    }

}

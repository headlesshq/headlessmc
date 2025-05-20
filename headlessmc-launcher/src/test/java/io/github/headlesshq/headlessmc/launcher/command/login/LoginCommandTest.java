package io.github.headlesshq.headlessmc.launcher.command.login;

import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.LauncherMock;
import io.github.headlesshq.headlessmc.launcher.command.LaunchContext;
import net.raphimc.minecraftauth.step.java.StepMCProfile;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoginCommandTest {
    @Test
    public void testLoginCommand() {
        Launcher launcher = LauncherMock.INSTANCE;
        new ArrayList<>(launcher.getAccountManager().getAccounts()).forEach(acc -> launcher.getAccountManager().removeAccount(acc));
        assertTrue(launcher.getAccountManager().getAccounts().isEmpty());

        try {
            launcher.getCommandLine().setCommandContext(new LaunchContext(launcher));
            boolean ran = false;
            for (Command command : launcher.getCommandLine().getCommandContext()) {
                if (command instanceof LoginCommand) {
                    LoginCommand loginCommand = (LoginCommand) command;
                    StepFullJavaSession.FullJavaSession session = new StepFullJavaSession.FullJavaSession(new StepMCProfile.MCProfile(UUID.randomUUID(), "", "", null), null);
                    loginCommand.onSuccessfulLogin(session);
                    assertTrue(launcher.getAccountManager().getAccounts().stream().anyMatch(acc -> acc.getSession() == session));
                    ran = true;
                }
            }

            assertTrue(ran);
        } finally {
            new ArrayList<>(launcher.getAccountManager().getAccounts()).forEach(acc -> launcher.getAccountManager().removeAccount(acc));
        }
    }

}

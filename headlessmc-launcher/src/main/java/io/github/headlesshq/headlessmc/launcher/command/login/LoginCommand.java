package io.github.headlesshq.headlessmc.launcher.command.login;

import io.github.headlesshq.headlessmc.auth.AbstractLoginCommand;
import io.github.headlesshq.headlessmc.auth.LoginState;
import io.github.headlesshq.headlessmc.auth.ValidatedAccount;
import io.github.headlesshq.headlessmc.launcher.ILauncher;
import io.github.headlesshq.headlessmc.launcher.auth.AuthException;
import jakarta.inject.Inject;
import lombok.CustomLog;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;

@CustomLog
public class LoginCommand extends AbstractLoginCommand {
    private final ILauncher launcher;

    @Inject
    public LoginCommand(ILauncher launcher, LoginState state) {
        super(launcher, state);
        this.launcher = launcher;
    }

    @Override
    protected void onSuccessfulLogin(StepFullJavaSession.FullJavaSession session) {
        ValidatedAccount validatedAccount;
        try {
            validatedAccount = launcher.getAccountManager().getAccountValidator().validate(session);
        } catch (AuthException e) {
            launcher.log(e.getMessage()); // TODO: throw CommandException?!
            return;
        }

        launcher.log("Logged into account " + validatedAccount.getName() + " successfully!");
        launcher.getAccountManager().addAccount(validatedAccount);
    }

}

package io.github.headlesshq.headlessmc.commands.auth;

import io.github.headlesshq.headlessmc.auth.AuthService;
import io.github.headlesshq.headlessmc.auth.Authenticator;
import io.github.headlesshq.headlessmc.auth.LastUsedAccountService;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine;

/**
 * legacy: in HeadlessMc 2 there was a 'headlessmc login' command.
 */
@Getter
@Setter
@CommandLine.Command(
    name = "login",
    hidden = true,
    mixinStandardHelpOptions = true,
    description = "Log into an account."
)
public class LoginShortcutCommand extends AbstractAccountCommand {
    @CommandLine.Option(
        names = {"--method"},
        description = "The authentication method to use (e.g. default, credentials, webview...)",
        defaultValue = Authenticator.DEFAULT_METHOD
    )
    private String method = Authenticator.DEFAULT_METHOD;

    public LoginShortcutCommand(
        LastUsedAccountService lastUsedAccountService,
        TableProvider tableProvider,
        AuthService authService,
        Console console
    ) {
        super(lastUsedAccountService, tableProvider, authService, console);
    }

    @Override
    public void run() {
        LoginCommand loginCommand = new LoginCommand();
        loginCommand.setContext(this);
        loginCommand.setMethod(method);
        loginCommand.run();
    }

}

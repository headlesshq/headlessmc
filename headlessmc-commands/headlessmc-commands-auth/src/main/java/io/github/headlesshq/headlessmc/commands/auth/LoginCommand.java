package io.github.headlesshq.headlessmc.commands.auth;

import io.github.headlesshq.headlessmc.auth.*;
import io.github.headlesshq.headlessmc.console.Console;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine;

@Getter
@Setter
@CommandLine.Command(
    name = "login",
    mixinStandardHelpOptions = true,
    description = "Log into an account."
)
public class LoginCommand implements AccountSubCommand {
    @CommandLine.ParentCommand
    private AccountCommandContext context;

    @CommandLine.Option(
        names = {"--method"},
        description = "The authentication method to use (e.g. default, credentials, webview...)",
        defaultValue = Authenticator.DEFAULT_METHOD
        // TODO: verify difficult to make suggestions as we dont know value of --provider yet?
    )
    private String method = Authenticator.DEFAULT_METHOD;

    // TODO: should be param?

    @Override
    public void run() {
        AuthProvider provider = getContext().getAuthProvider();
        Authenticator.Method<Console> method = provider.getAuthenticator().getMethods().get(getMethod());
        if (method == null) {
            throw new IllegalArgumentException(String.format(
                "Failed to find method %s, available: %s",
                getMethod(), provider.getAuthenticator().getMethods().keySet()
            ));
        }

        try {
            Account account = method.login(getContext().getConsole());
            context.getLastUsedAccountService().setLastUsedAccount(account);
            getContext().getConsole().write("Logged in to account " + account.getName() + " successfully!");
        } catch (AuthException e) {
            if (Authenticator.METHOD_WEBVIEW.equalsIgnoreCase(getMethod())
                && (e.getCause() instanceof NoClassDefFoundError || e.getCause() instanceof ClassNotFoundException)) {
                throw new AuthException("Login with WebView is unsupported on this JVM", e);
            }

            throw e;
        }
    }

}

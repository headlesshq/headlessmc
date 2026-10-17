package io.github.headlesshq.headlessmc.commands.auth;

import io.github.headlesshq.headlessmc.auth.AuthService;
import io.github.headlesshq.headlessmc.auth.LastUsedAccountService;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import picocli.CommandLine;

@CommandLine.Command(
    name = "account",
    aliases = {"auth"},
    mixinStandardHelpOptions = true,
    description = "Manage your account.",
    subcommands = {
        LoginCommand.class,
        ListCommand.class,
        RemoveCommand.class,
        SelectCommand.class,
        RefreshCommand.class
    }
)
public class AccountCommand extends AbstractAccountCommand implements CachedConsole.Enabled {
    public AccountCommand(
        LastUsedAccountService lastUsedAccountService,
        TableProvider tableProvider,
        AuthService authService,
        Console console
    ) {
        super(lastUsedAccountService, tableProvider, authService, console);
    }

    @Override
    public void run() {
        ListCommand listCommand = new ListCommand();
        listCommand.setContext(this);
        listCommand.run();
    }

}

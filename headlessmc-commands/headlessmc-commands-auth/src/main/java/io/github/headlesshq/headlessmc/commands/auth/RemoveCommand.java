package io.github.headlesshq.headlessmc.commands.auth;

import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

@Getter
@Setter
@CommandLine.Command(
    name = "remove",
    aliases = {"rm"},
    mixinStandardHelpOptions = true,
    description = "Removes an account."
)
public class RemoveCommand implements AccountSubCommand, CachedConsole.Enabled {
    @CommandLine.ParentCommand
    private AccountCommandContext context;

    @CommandLine.Parameters(
        description = "The name of the account to remove.",
        completionCandidates = AccountCompletions.class
    )
    private @Nullable String name;

    @Override
    public void run() {
        Account account = getContext().getAccount(name, "remove");
        getContext().getAuthProvider().removeAccount(account);
    }

}

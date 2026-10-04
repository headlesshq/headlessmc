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
    name = "select",
    mixinStandardHelpOptions = true,
    description = "Select the primary account to use."
)
public class SelectCommand implements AccountSubCommand, CachedConsole.Enabled {
    @CommandLine.ParentCommand
    private AccountCommandContext context;

    @CommandLine.Parameters(
        description = "The name of the account to select.",
        completionCandidates = AccountCompletions.class
    )
    private @Nullable String name;

    @Override
    public void run() {
        Account account = getContext().getAccount(name, "select");
        getContext().getLastUsedAccountService().setLastUsedAccount(account);
    }

}

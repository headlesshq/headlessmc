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
    name = "refresh",
    mixinStandardHelpOptions = true,
    description = "Refreshes an accounts token."
)
public class RefreshCommand implements AccountSubCommand, CachedConsole.Enabled {
    @CommandLine.ParentCommand
    private AccountCommandContext context;

    @CommandLine.Parameters(
        description = "The name of the account to refresh.",
        completionCandidates = AccountCompletions.class
    )
    private @Nullable String name;

    @Override
    public void run() {
        Account account = getContext().getAccount(name, "refresh");
        account = getContext().getAuthProvider().refresh(account);
        getContext().getLastUsedAccountService().setLastUsedAccount(account);
    }

}

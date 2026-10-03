package io.github.headlesshq.headlessmc.commands.auth;

import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.auth.AuthProvider;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine;

import java.util.List;

@Getter
@Setter
@CommandLine.Command(
    name = "list",
    aliases = {"ls"},
    mixinStandardHelpOptions = true,
    description = "List your accounts."
)
public class ListCommand implements AccountSubCommand, CachedConsole.Enabled {
    @CommandLine.ParentCommand
    private AccountCommandContext context;

    @CommandLine.Option(
        names = {"--methods"},
        description = "Lists the available login methods for a provider.",
        defaultValue = "false"
    )
    private boolean listMethods;

    @Override
    public void run() {
        AuthProvider provider = getContext().getAuthProvider();
        if (listMethods) {
            List<AuthProvider> providers = getContext().getAuthProviders();
            String table = getContext().getTableProvider().<AuthProvider>get()
                .withColumn("provider", AuthProvider::getName)
                .withColumn("methods", p -> p.getAuthenticator().getMethods().keySet().toString())
                .addAll(providers)
                .toString();

            getContext().getConsole().write(table);
            return;
        }

        List<Account> accounts = provider.getAccounts();
        String table = getContext().getTableProvider().<Account>get()
            .withColumn("name", Account::getName)
            .withColumn("uuid", Account::getUuid)
            .withColumn("type", Account::getType)
            .withColumn("provider", Account::getProvider)
            .addAll(accounts)
            .toString();

        getContext().getConsole().write(table);
    }

}

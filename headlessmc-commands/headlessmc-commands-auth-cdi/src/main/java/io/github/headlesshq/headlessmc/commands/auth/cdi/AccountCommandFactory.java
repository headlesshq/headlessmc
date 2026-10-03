package io.github.headlesshq.headlessmc.commands.auth.cdi;

import io.github.headlesshq.headlessmc.auth.AuthService;
import io.github.headlesshq.headlessmc.auth.LastUsedAccountService;
import io.github.headlesshq.headlessmc.commands.auth.*;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

@ApplicationScoped
// just to be sure @ParentCommand injection works
// not sure if this is actually needed
@RegisterForReflection(targets = {
    AccountCommandContext.class,
    AccountCommand.class,
    LoginCommand.class,
    ListCommand.class,
    RemoveCommand.class,
    SelectCommand.class,
    RefreshCommand.class
})
public class AccountCommandFactory {
    @Produces
    @Default
    @Dependent
    @Named("command:account") // prevent quarkus from removing
    public AccountCommand accountCommand(
        LastUsedAccountService lastUsedAccountService,
        TableProvider tableProvider,
        AuthService authService,
        Console console
    ) {
        return new AccountCommand(lastUsedAccountService, tableProvider, authService, console);
    }

    @Produces
    @Default
    @Dependent
    @Named("headlessmc:completions:account")
    public AccountCompletions completions(AuthService authService) {
        return new AccountCompletions(authService);
    }

    @Produces
    @Default
    @Dependent
    @Named("headlessmc:completions:authprovider")
    public AuthProviderCompletions authProviderCompletions(AuthService authService) {
        return new AuthProviderCompletions(authService);
    }

    // sub-commands do not need any injection

}

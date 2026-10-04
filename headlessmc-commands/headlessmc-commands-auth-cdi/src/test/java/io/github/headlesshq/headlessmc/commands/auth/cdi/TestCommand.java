package io.github.headlesshq.headlessmc.commands.auth.cdi;

import io.github.headlesshq.headlessmc.commands.auth.AccountCommand;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine;

@SuppressWarnings("unused")
@TopCommand
@Getter
@Setter
@CommandLine.Command(
    name = "test",
    mixinStandardHelpOptions = true,
    subcommands = AccountCommand.class
)
public class TestCommand {

}

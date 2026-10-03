package io.github.headlesshq.headlessmc.commands;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine;

@Getter
@Setter
@Dependent
@Named("command:exit")
@CommandLine.Command(
    name = "exit",
    aliases = "quit",
    description = "Exits the HeadlessMc shell",
    mixinStandardHelpOptions = true
)
class ExitCommand implements Runnable {
    @Override
    public void run() {
        System.exit(0);
    }

}

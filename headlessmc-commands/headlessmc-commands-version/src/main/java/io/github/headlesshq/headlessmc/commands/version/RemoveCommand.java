package io.github.headlesshq.headlessmc.commands.version;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

@Slf4j
@Getter
@Setter
@Default
@Dependent
@Named("command:version:remove")
@CommandLine.Command(
    name = "remove",
    aliases = {"rm", "uninstall"},
    mixinStandardHelpOptions = true,
    description = "Allows you to uninstall installed versions."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class RemoveCommand {
    // TODO: remove version
    // TODO: also remove game dir then?

}

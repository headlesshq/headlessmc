package io.github.headlesshq.headlessmc.commands.server;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.server.ServerService;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import picocli.CommandLine;

import java.util.List;

@Getter
@Setter
@Default
@Dependent
@Named("command:server:list")
@CommandLine.Command(
    name = "list",
    aliases = {"ls"},
    mixinStandardHelpOptions = true,
    description = "Lists your servers."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ListCommand implements Runnable, CachedConsole.Enabled {
    private final ServerService serverService;
    private final TableProvider tableProvider;
    private final Console console;

    @Override
    public void run() {
        List<Profile> servers = serverService.listServers();
        tableProvider.<Profile>get()
            .withColumn("name", Profile::name)
            .withColumn("version", server -> server.version().toString())
            // TODO: this is absolute path, if its in HeadlessMc/mc-dir, cut the start off
            .withColumn("directory", server -> server.path().toString())
            .addAll(servers)
            .log(console::write);
    }

}

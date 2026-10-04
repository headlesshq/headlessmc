package io.github.headlesshq.headlessmc.commands.server;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.server.ServerService;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

@Getter
@Setter
@Default
@Dependent
@Named("command:server:remove")
@CommandLine.Command(
    name = "remove",
    aliases = {"rm"},
    mixinStandardHelpOptions = true,
    description = "Removes a server."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class RemoveCommand implements Runnable, CachedConsole.Enabled {
    private final ServerService serverService;
    private final FileService fileService;
    private final Console console;

    @CommandLine.Parameters(
        description = "The name of the server to remove.",
        completionCandidates = ServerCompletions.class
    )
    private @Nullable String name;

    @Override
    public void run() {
        if (name == null) {
            throw new IllegalArgumentException("Please specify the name of the server to remove.");
        }

        Profile server = serverService.getServer(name)
            .orElseThrow(() -> new IllegalArgumentException("Failed to find server with name " + name));

        serverService.remove(server);
        fileService.delete(server.path());
        console.write("Removed server " + server.name());
    }

}

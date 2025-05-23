package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.logging.StdIO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@ApplicationScoped
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class CommandLineManagerImpl implements CommandLineManager {
    private final PicocliCommandContext context;
    private final CommandLineReader reader;
    private final StdIO stdIO;

    private volatile CommandContext interactiveContext;

}

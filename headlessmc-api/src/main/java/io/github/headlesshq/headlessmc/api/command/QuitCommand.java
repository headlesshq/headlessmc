package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.exit.ExitManager;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import picocli.CommandLine.Command;

import javax.inject.Inject;

/**
 * A command that calls HeadlessMc's {@link ExitManager} to quit the process.
 */
@Command(
        name = "quit",
        mixinStandardHelpOptions = true,
        description = "Quits HeadlessMc."
)
@Setter
@NoArgsConstructor
@AllArgsConstructor(onConstructor = @__(@Inject))
public class QuitCommand implements Runnable {
    @Inject
    private HeadlessMc ctx;

    @Override
    public void run() {
        ctx.log("Bye!");
        ctx.getExitManager().exit(0);
    }

}

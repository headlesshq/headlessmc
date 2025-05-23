package io.github.headlessmc.headlesshq.weld;

import io.github.headlesshq.headlessmc.api.HeadlessMc;
import picocli.CommandLine;

@CommandLine.Command(
        name = "headlessmc",
        version = HeadlessMc.NAME_VERSION,
        mixinStandardHelpOptions = true,
        description = "HeadlessMcCommand"
)
public class HeadlessMcCommand {

}

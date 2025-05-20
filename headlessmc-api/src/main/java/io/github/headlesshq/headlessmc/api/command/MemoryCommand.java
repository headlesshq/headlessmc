package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.HeadlessMc;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import picocli.CommandLine.Command;

import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * A command that displays the currently used memory, free memory and max memory of the JVM.
 */
@Command(
        name = "memory",
        mixinStandardHelpOptions = true,
        description = "Displays Memory stats."
)
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemoryCommand implements Runnable {
    private static final DecimalFormat DF = new DecimalFormat("#.##");

    static {
        DF.setRoundingMode(RoundingMode.CEILING);
    }

    private HeadlessMc ctx;

    @Override
    public void run() {
        long total = (Runtime.getRuntime().totalMemory() / 1024L / 1024L);
        long free = (Runtime.getRuntime().freeMemory() / 1024L / 1024L);
        long max = (Runtime.getRuntime().maxMemory() / 1024L / 1024L);
        long usedMemory = total - free;
        String percent = DF.format(usedMemory * 100L / (double) max);
        ctx.log("-Used:  " + usedMemory + "mb, (" + percent + "%)");
        ctx.log("-Free:  " + free + "mb");
        ctx.log("-Total: " + total + "mb");
        ctx.log("-Max:   " + max + "mb");
    }

}

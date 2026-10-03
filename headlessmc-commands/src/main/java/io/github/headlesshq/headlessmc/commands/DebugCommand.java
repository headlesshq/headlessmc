package io.github.headlesshq.headlessmc.commands;

import io.github.headlesshq.headlessmc.HeadlessMc;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.McFiles;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import picocli.CommandLine;

import java.util.Locale;

@Getter
@Setter
@Default
@Dependent
@Named("command:debug")
@CommandLine.Command(
    name = "debug",
    mixinStandardHelpOptions = true,
    hidden = true,
    description = "Output debugging information."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DebugCommand implements Runnable, CachedConsole.Enabled {
    private final AppFiles appFiles;
    private final McFiles mcFiles;
    private final Console console;

    @Override
    public void run() {
        console.write("------------------------------------------");
        console.write(HeadlessMc.NAME + " - " + HeadlessMc.VERSION);
        console.write("------------------------------------------");
        console.write("Files:");
        console.write(" - Data:   " + appFiles.getDataDir().toAbsolutePath());
        console.write(" - Config: " + appFiles.getConfigDir().toAbsolutePath());
        console.write(" - State:  " + appFiles.getStateDir().toAbsolutePath());
        console.write(" - Cache:  " + appFiles.getCacheDir().toAbsolutePath());
        console.write(" - Mc:     " + mcFiles.getMcDir().toAbsolutePath());
        console.write("------------------------------------------");
        console.write("Memory:");
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long used = total - free;

        console.write(" - Max:   " + mb(max) + " MB");
        console.write(" - Total: " + mb(total) + " MB");
        console.write(" - Used:  " + mb(used) + " MB");
        console.write(" - Free:  " + mb(free) + " MB");
        console.write("------------------------------------------");
    }

    private String mb(long bytes) {
        if (bytes == Long.MAX_VALUE) {
            return "unlimited";
        }

        return String.format(Locale.ROOT, "%.2f", bytes / 1024.0D / 1024.0D);
    }

}

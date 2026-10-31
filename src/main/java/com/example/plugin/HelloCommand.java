package com.example.plugin;

import com.github.lalyos.jfiglet.FigletFont;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.plugins.HeadlessMcSubcommand;
import io.quarkus.runtime.annotations.RegisterResources;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Adds {@code headlessmc hello <name>}.
 * Every {@link HeadlessMcSubcommand} bean becomes a subcommand of {@code headlessmc}.
 * Plugins are regular CDI beans, so they can inject everything HeadlessMc provides.
 * <p>
 * For {@code headlessmc plugin compile}, which compiles HeadlessMc with plugins into a native image,
 * resources that are read at runtime have to be registered, here the font jfiglet loads.
 */
@Dependent
@RegisterResources(globs = "standard.flf")
@CommandLine.Command(name = "hello", mixinStandardHelpOptions = true, description = "Says hello.")
public class HelloCommand implements Runnable, HeadlessMcSubcommand {
    private final Console console;

    @CommandLine.Parameters(paramLabel = "NAME", description = "Who to greet.", defaultValue = "world")
    private String name;

    @CommandLine.Option(names = {"-b", "--banner"}, description = "Prints the greeting as ASCII art.")
    private boolean banner;

    @Inject
    public HelloCommand(Console console) {
        this.console = console;
    }

    @Override
    public void run() {
        String greeting = "Hello, " + name + "!";
        if (banner) {
            try {
                // jfiglet is nested in the plugin jar, see the include dependency in build.gradle
                greeting = FigletFont.convertOneLine(greeting);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        console.write(greeting);
    }

}

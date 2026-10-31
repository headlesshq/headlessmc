package io.github.headlesshq.headlessmc.plugins;

/**
 * Marks a picocli {@code @CommandLine.Command} bean that should be added
 * as a subcommand of the {@code headlessmc} command, e.g. by a plugin:
 * <pre>{@code
 * @Dependent
 * @CommandLine.Command(name = "hello", description = "Says hello.")
 * public class HelloCommand implements Runnable, HeadlessMcSubcommand {
 *     ...
 * }
 * }</pre>
 */
public interface HeadlessMcSubcommand {

}

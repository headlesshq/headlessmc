package io.github.headlesshq.headlessmc.commands;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.files.FileService;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Prints the completion script for a shell, which completes by calling {@link ShellCompletions}.
 */
@Getter
@Setter
@Dependent
@Named("command:completion")
@CommandLine.Command(
    name = "completion",
    hidden = true,
    mixinStandardHelpOptions = true,
    description = "Prints the completion script for a shell.",
    footer = {
        "%nzsh, in ~/.zshrc after compinit:",
        "  source <(headlessmc completion zsh)",
        "bash, in ~/.bashrc:",
        "  source <(headlessmc completion bash)",
        "fish:",
        "  headlessmc completion fish -o ~/.config/fish/completions/headlessmc.fish",
        "PowerShell, in $PROFILE:",
        "  headlessmc completion powershell | Out-String | Invoke-Expression",
        "%nSee https://headlesshq.github.io/headlessmc/getting-started/#shell-completions"
    }
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class CompletionCommand implements Runnable, CachedConsole.Enabled {
    private final FileService fileService;
    private final Console console;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @CommandLine.Parameters(index = "0", description = "The shell, one of: ${COMPLETION-CANDIDATES}.")
    private Shell shell;

    @CommandLine.Option(
        names = {"-o", "--output"},
        description = "Writes the script to this file instead, overwriting it and creating its directories."
    )
    private @Nullable Path output;

    @Override
    public void run() {
        String resource = "completions/headlessmc." + shell.name().toLowerCase(Locale.ROOT);
        try (InputStream is = CompletionCommand.class.getResourceAsStream(resource)) {
            if (is == null) {
                throw new IllegalStateException("Failed to find completion script " + resource);
            }

            if (output == null) {
                PrintWriter out = spec.commandLine().getOut();
                out.print(new String(is.readAllBytes(), StandardCharsets.UTF_8));
                out.flush();
            } else {
                fileService.create(output, is);
                console.write("Wrote the " + shell + " completion script to " + output.toAbsolutePath());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public enum Shell {
        bash,
        zsh,
        fish,
        powershell
    }

}

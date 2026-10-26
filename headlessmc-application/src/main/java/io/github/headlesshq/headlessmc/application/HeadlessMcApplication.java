package io.github.headlesshq.headlessmc.application;

import io.github.headlesshq.headlessmc.config.ConfigFileProvider;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.updater.UpdateNotifier;
import io.quarkus.runtime.QuarkusApplication;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class HeadlessMcApplication implements QuarkusApplication {
    private final CommandLine commandLine;
    private final FileService fileService;
    private final AppFiles appFiles;
    private final ConfigFileProvider configFileProvider;
    private final UpdateNotifier updateNotifier;

    @Override
    public int run(String... args) throws Exception {
        initFiles();
        commandLine.setUsageHelpAutoWidth(true);
        updateNotifier.checkBefore(getCommand(args));
        return commandLine.execute(args);
    }

    /**
     * Determines the command that the given arguments select, without executing it.
     *
     * @param args the arguments HeadlessMc has been started with.
     * @return the selected (sub)command, or {@code null} if the arguments could not be parsed
     * or only ask for the usage help or the version.
     */
    private @Nullable Object getCommand(String... args) {
        try {
            CommandLine.ParseResult parseResult = commandLine.parseArgs(args);
            while (true) {
                if (parseResult.isUsageHelpRequested() || parseResult.isVersionHelpRequested()) {
                    return null;
                } else if (!parseResult.hasSubcommand()) {
                    return parseResult.commandSpec().userObject();
                }

                parseResult = parseResult.subcommand();
            }
        } catch (CommandLine.ParameterException e) {
            return null;
        }
    }

    private void initFiles() {
        try {
            Files.createDirectories(appFiles.getDataDir());
            Files.createDirectories(appFiles.getConfigDir());
            fileService.ensureFileExists(
                configFileProvider.getConfigFile(),
                ("# === HeadlessMc Config ==="
                    + System.lineSeparator()
                    + "# hmc.jline.enabled=true"
                ).getBytes(StandardCharsets.UTF_8)
            );
        } catch (IOException e) {
            log.error("Failed to initialize app files", e);
        }
    }

}

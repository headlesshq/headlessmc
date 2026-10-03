package io.github.headlesshq.headlessmc.application;

import io.github.headlesshq.headlessmc.config.ConfigFileProvider;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.quarkus.runtime.QuarkusApplication;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public int run(String... args) throws Exception {
        initFiles();
        commandLine.setUsageHelpAutoWidth(true);
        return commandLine.execute(args);
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

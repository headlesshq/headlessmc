package io.github.headlesshq.headlessmc.launcher.logging;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.net.DownloadService;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.util.TemplateString;
import io.github.headlesshq.headlessmc.version.util.TemplateStrings;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class LoggingDownloader {
    final DownloadService downloadService;
    final Holder<LoggingConfig> config;
    final FileService fileService;
    final McFiles mcFiles;

    public Optional<String> downloadLogging(TemplateStrings templateStrings, Version version) {
        Map<String, Version.LoggingConfiguration> loggingConfigurations = version.getLogging();
        if (loggingConfigurations == null) {
            return Optional.empty();
        }

        Version.LoggingConfiguration configuration = loggingConfigurations.get(Version.LOGGING_CLIENT);
        if (configuration == null) {
            return Optional.empty();
        }

        return Optional.of(downloadLogging(templateStrings, configuration));
    }

    private String downloadLogging(TemplateStrings templates, Version.LoggingConfiguration configuration) {
        Version.Download download = configuration.getFile();
        Path configFile = fileService.getPath(
            mcFiles.getAssetsDir(),
            "log_configs",
            requireNonNull(download.getId(), "Logging id was null: " + configuration)
        );

        if (!Files.exists(configFile)) {
            downloadService.download(
                    URI.create(requireNonNull(download.getUrl(), "Logging url was null: " + configuration))
                ).sha1(download.getSha1())
                .size(download.getSize())
                .toFile(configFile);
        }

        configFile = patch(configFile);
        templates.add(TemplateString.LOGGING_PATH, configFile.toAbsolutePath().toString());
        return templates.process(configuration.getArgument());
    }

    // by default the logging.xml files from mojang log to the STD_OUT in an xml format, we want to change that.
    private Path patch(Path configFile) {
        if (config.get().patch()) {
            // maybe this should be hash-size-patched.xml?
            Path patched = configFile.getParent().resolve(configFile.getFileName().toString() + "-patched.xml");
            if (Files.exists(patched)) {
                return patched;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(configFile)))) {
                String before = br.readAllAsString();
                String text = before.replace( // legacy client-1.7.xml
                    "<XMLLayout />",
                    "<PatternLayout pattern=\"[%d{HH:mm:ss}] [%t/%level]: %msg%n\" />"
                ).replace( // client-1.12.xml and client-1.21.2.xml
                    "<LegacyXMLLayout />",
                    "<PatternLayout pattern=\"[%d{HH:mm:ss}] [%t/%level]: %msg{nolookups}%n\"/>"
                );

                if (before.equals(text)) {
                    log.error("Failed to patch logging.xml file {}: {}", configFile, text);
                    return configFile;
                }

                try (OutputStream outputStream = Files.newOutputStream(patched)) {
                    outputStream.write(text.getBytes(StandardCharsets.UTF_8));
                    return patched;
                }
            } catch (IOException e) {
                throw new HeadlessMcIOException("Failed to patch logging config " + configFile, e);
            }
        }

        return configFile;
    }

}

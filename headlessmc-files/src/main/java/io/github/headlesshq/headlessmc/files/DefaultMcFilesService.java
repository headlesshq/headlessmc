package io.github.headlesshq.headlessmc.files;

import io.github.headlesshq.headlessmc.config.ConfigService;
import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.os.OS;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@link McFilesService} of the application.
 * The primary directory is determined by {@link DefaultMcFiles},
 * additional directories are read from {@link FileConfig#mcDirs()}.
 * Both follow changes of the configuration at runtime.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DefaultMcFilesService extends AbstractMcFilesService {
    private final Holder<FileConfig> configHolder;
    private final ConfigService configService;
    private final FileService fileService;
    private final McDirDetector detector;
    private final OS os;

    @Override
    public McFiles getPrimary() {
        return all().getFirst();
    }

    @Override
    public List<McFiles> all() {
        return configService.bind("DefaultMcFilesService.all", this::computeAll);
    }

    private List<McFiles> computeAll() {
        List<McFiles> result = new ArrayList<>();
        result.add(McFiles.of(new DefaultMcFiles(configHolder, configService, fileService, os).getMcDir()));
        for (String mcDir : configHolder.get().mcDirs().orElse(List.of())) {
            if (mcDir.isBlank()) {
                continue;
            }

            McFiles mcFiles = McFiles.of(fileService.getUserPath(mcDir.trim()));
            if (result.stream().anyMatch(mcFiles::isSame)) {
                log.debug("Minecraft directory {} is configured multiple times", mcFiles.getMcDir());
                continue;
            }

            warnIfNested(result, mcFiles);
            result.add(mcFiles);
        }

        addDetected(result);
        return List.copyOf(result);
    }

    /**
     * Adds the directories found on other drives, after the configured ones.
     * This is computed with the list of all directories, so a drive is searched once and not for every command.
     */
    private void addDetected(List<McFiles> result) {
        FileConfig config = configHolder.get();
        if (!config.mcDetect()) {
            return;
        }

        for (Path mcDir : detector.detect(Math.max(1, config.mcDetectDepth()))) {
            McFiles mcFiles = McFiles.of(mcDir);
            boolean known = result.stream().anyMatch(other ->
                other.isSame(mcFiles) || McFiles.normalize(mcDir).startsWith(McFiles.normalize(other.getMcDir())));
            if (!known) {
                log.info("Found Minecraft directory {}", mcDir);
                result.add(mcFiles);
            }
        }
    }

    private void warnIfNested(List<McFiles> others, McFiles mcFiles) {
        Path mcDir = McFiles.normalize(mcFiles.getMcDir());
        for (McFiles other : others) {
            Path otherDir = McFiles.normalize(other.getMcDir());
            if (mcDir.startsWith(otherDir) || otherDir.startsWith(mcDir)) {
                log.warn("Minecraft directories {} and {} are nested in each other, this is not recommended",
                         other.getMcDir(), mcFiles.getMcDir());
            }
        }
    }

}

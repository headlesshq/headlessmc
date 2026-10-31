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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DefaultMcFilesService implements McFilesService {
    private final Holder<FileConfig> configHolder;
    private final ConfigService configService;
    private final FileService fileService;
    private final OS os;

    @Override
    public McFiles getPrimaryMcFiles() {
        return getAllMcFiles().getFirst();
    }

    @Override
    public List<McFiles> getAllMcFiles() {
        return configService.bind("DefaultMcFilesService.computeMcFiles", this::computeMcFiles);
    }

    private List<McFiles> computeMcFiles() {
        Path primary = computeMcDir();
        Set<Path> normalized = new LinkedHashSet<>();
        normalized.add(primary.toAbsolutePath().normalize());

        List<McFiles> result = new ArrayList<>();
        result.add(McFiles.of(primary));
        for (String folder : configHolder.get().mcFolders().orElse(List.of())) {
            if (folder.isBlank()) {
                continue;
            }

            Path path = fileService.getUserPath(folder.trim());
            if (normalized.add(path.toAbsolutePath().normalize())) {
                result.add(McFiles.of(path));
            } else {
                log.warn("Configured .minecraft folder {} twice", path);
            }
        }

        return List.copyOf(result);
    }

    private Path computeMcDir() {
        FileConfig config = configHolder.get();
        Optional<String> legacyMcDir = configService.getConfig().getOptionalValue("hmc.mcdir", String.class);
        Optional<String> mcDir = config.property(
            config.mcDir(),
            legacyMcDir,
            log::warn,
            "hmc.mcdir",
            "hmc.files.mc"
        );

        if (mcDir.isPresent()) {
            return fileService.getUserPath(mcDir.get());
        }

        if (OS.Type.WINDOWS.equals(os.type())) {
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                return fileService.getUserPath(appData).resolve(".minecraft");
            } else {
                log.warn("Failed to find APPDATA");
            }
        }

        String userHomeProperty = System.getProperty("user.home");
        if (userHomeProperty == null) {
            log.error("user.home not specified");
        }

        Path userHome = userHomeProperty != null ? fileService.getUserPath(userHomeProperty) : fileService.getUserPath();
        if (OS.Type.MACOS.equals(os.type())) {
            return userHome.resolve("Library").resolve("Application Support").resolve("minecraft");
        }

        return userHome.resolve(".minecraft");
    }

}

package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.config.ConfigService;
import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileConfig;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Optional;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class GameDirService {
    private final Holder<FileConfig> config;
    private final ConfigService configService;
    private final FileService fileService;
    private final AppFiles appFiles;
    private final McFiles mcFiles;

    public Path getGameDir(VersionArg version) {
        FileConfig config = this.config.get();
        if (Side.SERVER.equals(version.side().orElse(Side.CLIENT))) {
            // TODO: not super happy with this, should be placed deeper,
            //  make a distinction between default profiles and explicit profiles
            return appFiles.getServerDir().resolve("server-" + version.toString("-"));
        }

        Optional<String> legacyGameDir = configService.getConfig().getOptionalValue("hmc.gamedir", String.class);
        Optional<String> gameDir = config.property(
            config.gameDir(),
            legacyGameDir,
            log::warn,
            "hmc.gamedir",
            "hmc.files.game"
        );

        if (gameDir.isPresent()) {
            return fileService.getUserPath(gameDir.get());
        }

        if (config.gameForEachVersion()) {
            return mcFiles.getMcDir().resolve(version.toString("-"));
        }

        return mcFiles.getMcDir();
    }

}

package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.HeadlessMc;
import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.launcher.assets.AssetsLocation;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchOptions;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.version.ProcessedVersion;
import io.github.headlesshq.headlessmc.version.util.TemplateString;
import io.github.headlesshq.headlessmc.version.util.TemplateStrings;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class TemplateStringService {
    private final McFiles mcFiles;
    private final CPU cpu;

    public TemplateStrings create(
        AssetsLocation assets,
        Account account,
        Path gameDir,
        ProcessedVersion version,
        LaunchOptions options
    ) {
        TemplateStrings result = new TemplateStrings();
        result.add(TemplateString.ARCH, String.valueOf(cpu.bitness().bits()));
        result.add(TemplateString.ASSET_INDEX_NAME, assets.id());
        // .minecraft/assets
        // .minecraft/assets/virtual/legacy
        // .minecraft/resources on super-legacy, or:
        // .minecraft/assets/virtual/pre-1.6 (maybe a bug, see AssetsLocation Javadoc)
        result.add(TemplateString.ASSETS_ROOT, assets.location().toAbsolutePath().toString());
        result.add(TemplateString.GAME_ASSETS, assets.location().toAbsolutePath().toString());

        result.add(TemplateString.AUTH_ACCESS_TOKEN, account.getToken());
        result.add(TemplateString.AUTH_SESSION, account.getToken()); // this is the legacy version of auth_access_token
        result.add(TemplateString.AUTH_PLAYER_NAME, account.getName());
        result.add(TemplateString.AUTH_UUID, account.getUuid());
        result.add(TemplateString.AUTH_XUID, "");
        result.add(TemplateString.CLASSPATH_SEPARATOR, File.pathSeparator);
        result.add(TemplateString.CLIENT_ID, "");
        result.add(TemplateString.GAME_DIRECTORY, gameDir.toAbsolutePath().toString());
        result.add(TemplateString.LIBRARY_DIRECTORY, mcFiles.getLibraryDir().toAbsolutePath().toString());

        result.add(TemplateString.LAUNCHER_NAME, HeadlessMc.NAME); // official: minecraft-launcher
        result.add(TemplateString.LAUNCHER_VERSION, HeadlessMc.VERSION); // official: 3.35.10

        result.add(TemplateString.PROFILE_PROPERTIES, "{}");
        result.add(TemplateString.USER_PROPERTIES, "{}");
        result.add(TemplateString.USER_TYPE, "msa"); // TODO
        result.add(TemplateString.VERSION_NAME, version.getId());
        result.add(TemplateString.VERSION_TYPE, version.getType());

        options.quickPlayPath().ifPresent(path -> result.add(TemplateString.QUICK_PLAY_PATH, path));
        options.join().ifPresent(join -> {
            switch (join.type()) {
                case SERVER -> result.add(TemplateString.QUICK_PLAY_MULTIPLAYER, join.target());
                case SINGLEPLAYER -> result.add(TemplateString.QUICK_PLAY_SINGLEPLAYER, join.target());
                case REALMS -> result.add(TemplateString.QUICK_PLAY_REALMS, join.target());
            }
        });
        options.resolution().ifPresent(resolution -> {
            result.add(TemplateString.WIDTH, String.valueOf(resolution.width()));
            result.add(TemplateString.HEIGHT, String.valueOf(resolution.height()));
        });

        // result.add(TemplateString.LOGGING_PATH, ); done by LoggingDownloader
        // result.add(TemplateString.CLASSPATH, ); done later once classpath is built
        // result.add(TemplateString.NATIVES_DIRECTORY, ); done by NativeLibraryHandler

        return result;
    }

}

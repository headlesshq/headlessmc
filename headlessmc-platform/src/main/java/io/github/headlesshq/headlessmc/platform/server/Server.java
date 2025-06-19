package io.github.headlesshq.headlessmc.platform.server;

import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.platform.Mod;
import io.github.headlesshq.headlessmc.platform.Moddable;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.VersionID;
import lombok.CustomLog;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Data
@CustomLog
public class Server implements Moddable {
    public static final String DEFAULT_JAR = "server.jar";

    private final Platform platform;
    private final VersionID version;
    private final String name;
    private final Path path;

    public Path getEula(boolean inMemory) {
        return inMemory
                ? Paths.get("eula.txt") // if we launch in memory the working dir is here
                : path.resolve("eula.txt");
    }

    public boolean hasEula(boolean inMemory) {
        return Files.exists(getEula(inMemory));
    }

    public Path getExecutable(OS os) {
        Path jar = path.resolve(DEFAULT_JAR);
        if (!Files.exists(jar)) {
            if ("fabric".equalsIgnoreCase(version.getPlatform())) {
                jar = path.resolve("fabric-server-launch.jar");
            } else if (version.getPlatform().toLowerCase(Locale.ENGLISH).contains("forge")) {
                Path runFile = os.getType() == OS.Type.WINDOWS
                        ? path.resolve("run.bat")
                        : path.resolve("run.sh");
                if (Files.exists(runFile)) {
                    return runFile;
                }

                String name = getName(version.getPlatform(), version.getVersion(), version.getBuild())
                        .substring(1); // prevents capitalization issues like Forge vs forge
                try (Stream<Path> files = Files.list(path)) {
                    Path path = files.filter(f -> f.toString().endsWith(".jar"))
                            .filter(f -> f.getFileName().toString().contains(name))
                            .findFirst()
                            .orElse(null);
                    if (path != null) {
                        return path;
                    }
                } catch (IOException e) {
                    log.error(e);
                    return path.resolve(DEFAULT_JAR);
                }
            }
        }

        return jar;
    }

    public boolean hasCustomName() {
        return !name.startsWith(getName(version.getPlatform(), version.getVersion(), version.getBuild()));
    }

    public static String getName(String type, String version, @Nullable String build) {
        return type + "-" + version + (build == null ? "" : "-" + build);
    }

    // TODO: maybe the platform itself should do that?
    @Override
    public Path getModsDirectory() {
        if (getPlatform() == Platform.PURPUR || getPlatform() == Platform.PAPER) {
            return path.resolve("plugins");
        }

        return path.resolve("mods");
    }

}

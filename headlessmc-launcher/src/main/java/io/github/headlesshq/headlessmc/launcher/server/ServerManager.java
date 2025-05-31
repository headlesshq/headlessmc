package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.settings.Config;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.launcher.LazyService;
import io.github.headlesshq.headlessmc.launcher.api.Platform;
import io.github.headlesshq.headlessmc.launcher.api.VersionId;
import io.github.headlesshq.headlessmc.launcher.files.FileManager;
import io.github.headlesshq.headlessmc.launcher.modlauncher.Modlauncher;
import io.github.headlesshq.headlessmc.launcher.mods.ModdableGame;
import io.github.headlesshq.headlessmc.launcher.mods.ModdableGameProvider;
import io.github.headlesshq.headlessmc.launcher.server.downloader.*;
import lombok.Cleanup;
import lombok.CustomLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@Getter
@CustomLog
@RequiredArgsConstructor
public class ServerManager extends LazyService<Server> implements ModdableGameProvider {
    private final List<ServerType> serverTypes = new ArrayList<>();
    private final Config config;
    private final Path serversDir;
    private final boolean cache;

    @Override
    protected Collection<Server> update() {
        List<Server> servers = new ArrayList<>();
        Server testServer = getTestServer();
        if (testServer != null) {
            servers.add(testServer);
        }

        for (ServerType type : serverTypes) {
            Path typeDir = serversDir.resolve(type.getName());
            if (!Files.exists(typeDir)) {
                continue;
            }

            try (Stream<Path> files = Files.list(typeDir)) {
                for (Path mcVersion : itr(files.filter(Files::isDirectory))) {
                    @Cleanup
                    Stream<Path> builds = Files.list(mcVersion);
                    for (Path build : itr(builds.filter(Files::isDirectory))) {
                        @Cleanup
                        Stream<Path> serverDirs = Files.list(build);
                        for (Path serverDir : itr(serverDirs.filter(Files::isDirectory))) {
                            servers.add(new Server(
                                serverDir,
                                serverDir.getFileName().toString(),
                                new ServerVersion(
                                        type,
                                        mcVersion.getFileName().toString(),
                                        build.getFileName().toString()
                                ),
                                servers.size()
                            ));
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Failed to read files of " + type, e);
            }
        }

        return servers;
    }

    public @Nullable Server getServer(String serverName) {
        return getContents()
                .stream()
                .filter(s -> s.getName().equalsIgnoreCase(serverName))
                .findFirst()
                .orElse(null);
    }

    public void cache(Launcher launcher, Server server) throws IOException {
        Path cacheDir = getServerCacheDir(launcher);
        ServerVersion version = server.getVersion();
        Server cachedServer = findCachedServer(cacheDir, version.getServerType(), version.getVersion(), version.getTypeVersion());
        if (cachedServer != null) {
            log.info("Already cached server similar to server " + server.getName());
            return;
        }

        Path path = resolveServerPath(cacheDir, version.getServerType(), version.getVersion(), version.getTypeVersion(), null);
        FileManager.copyDirectory(server.getPath(), path);
    }

    public Path add(Launcher launcher,
                    ServerType type,
                    @Nullable String nameIn,
                    @Nullable String versionIn,
                    @Nullable String typeVersionIn,
                    String... args) throws IOException {
        String version;
        if (versionIn == null) {
            version = launcher
                    .getVersionInfoCache()
                    .cache(false)
                    .stream()
                    .filter(v -> "release".equalsIgnoreCase(v.getType()))
                    .findFirst()
                    .orElseThrow(() -> new IOException("Failed to find any Minecraft versions!"))
                    .getName();
        } else {
            version = versionIn;
        }

        Path cachedServer = checkCachedServers(launcher, type, version, typeVersionIn, nameIn);
        if (cachedServer != null) {
            refresh();
            return cachedServer;
        }

        launcher.log("Adding " + type.getName() + " server for " + version);
        ServerTypeDownloader.DownloadHandler downloadHandler =
                type.getDownloader().download(launcher, version, typeVersionIn, args);

        Path result = downloadHandler.download(typeVersion ->
                resolveServerPath(serversDir, type, version, typeVersion, nameIn));
        refresh();
        return result;
    }

    public void remove(Server server) throws IOException {
        FileManager fileManager = new FileManager(server.getPath().toAbsolutePath().toString());
        fileManager.delete(server.getPath().toFile());
        contents.remove(server);
        refresh();
    }

    public @Nullable ServerType getServerType(String name) {
        for (ServerType type : serverTypes) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }

        return null;
    }

    public static ServerManager create(Config config, FileManager launcherFileManager) {
        Path serversDir = launcherFileManager.createRelative("servers").getBase().toPath();
        ServerManager serverManager = new ServerManager(config, serversDir, false);
        serverManager.getServerTypes().add(new ServerType(Platform.PAPER, new PaperDownloader()));
        serverManager.getServerTypes().add(new ServerType(Platform.FABRIC, new FabricDownloader(new VanillaDownloader())));
        serverManager.getServerTypes().add(new ServerType(Platform.VANILLA, new VanillaDownloader()));
        serverManager.getServerTypes().add(new ServerType(Platform.PURPUR, new PurpurDownloader()));
        serverManager.getServerTypes().add(new ServerType(Platform.NEOFORGE, new ForgeDownloader(new VanillaDownloader(), Modlauncher.NEOFORGE)));
        serverManager.getServerTypes().add(new ServerType(Platform.FORGE, new ForgeDownloader(new VanillaDownloader(), Modlauncher.LEXFORGE)));
        return serverManager;
    }

    private <T> Iterable<T> itr(Stream<T> stream) {
        return stream::iterator;
    }

    private @Nullable Path checkCachedServers(Launcher launcher,
                                              ServerType type,
                                              String version,
                                              @Nullable String typeVersion,
                                              @Nullable String nameIn) throws IOException {
        if (!cache
                && launcher.getConfig().get(LauncherProperties.SERVER_TEST, false)
                && launcher.getConfig().get(LauncherProperties.SERVER_TEST_CACHE, false)) {
            Path serverCacheDir = getServerCacheDir(launcher);
            Server server = findCachedServer(serverCacheDir, type, version, typeVersion);
            if (server != null) {
                log.info("Restoring server cache " + server.getPath());
                Path path = resolveServerPath(serversDir, type, version, server.getVersion().getTypeVersion(), nameIn);
                FileManager.copyDirectory(server.getPath(), path);
                return path;
            }
        }

        return null;
    }

    private @Nullable Server findCachedServer(Path cacheDir,
                                              ServerType type,
                                              String version,
                                              @Nullable String typeVersion) {
        ServerManager cache = new ServerManager(config, cacheDir, true);
        cache.getServerTypes().add(type);
        cache.refresh();

        return cache.stream()
                .filter(s -> s.getVersion().getServerType().equals(type))
                .filter(s -> s.getVersion().getVersion().equals(version))
                .filter(s -> typeVersion == null
                        || s.getVersion().getTypeVersion().equals(typeVersion))
                .findFirst()
                .orElse(null);
    }

    private Path getServerCacheDir(Launcher launcher) {
        if (launcher.getConfig().get(LauncherProperties.SERVER_TEST_CACHE_USE_MC_DIR, false)) {
            return launcher.getMcFiles().getBase().toPath().resolve("servers");
        }

        return launcher.getFileManager().relative("servercache").getBase().toPath();
    }

    private String getNewServerName(ServerType type, String version, @Nullable String typeVersion) {
        String name = Server.getName(type, version, typeVersion);
        int i = 1;
        while (getServer(name) != null) {
            name = Server.getName(type, version, typeVersion) + "-" + i++;
        }

        return name;
    }

    private Path resolveServerPath(Path serversDir,
                                   ServerType type,
                                   String version,
                                   String typeVersion,
                                   String nameIn) throws IOException {
        String testDir = config.get(LauncherProperties.SERVER_TEST_DIR, null);
        if (!cache && testDir != null) {
            return Paths.get(testDir);
        }

        String name = nameIn;
        if (name == null) {
            name = getNewServerName(type, version, typeVersion);
        }

        Server server = getServer(name);
        if (server != null) {
            throw new IOException(String.format("Server %s already exists", name));
        }

        return serversDir
                .resolve(type.getName())
                .resolve(version)
                .resolve(typeVersion == null ? "latest" : typeVersion)
                .resolve(name);
    }

    private @Nullable Server getTestServer() {
        if (cache) {
            return null;
        }

        String testDir = config.get(LauncherProperties.SERVER_TEST_DIR, null);
        if (testDir == null) {
            return null;
        }

        Path testPath = Paths.get(testDir);
        if (!Files.isDirectory(testPath)) {
            return null;
        }

        String testTypeName = config.get(LauncherProperties.SERVER_TEST_TYPE, null);
        String testVersion = config.get(LauncherProperties.SERVER_TEST_VERSION, null);
        ServerType testType;
        if (testTypeName == null || testVersion == null || (testType = getServerType(testTypeName)) == null) {
            throw new IllegalArgumentException("Please specify override type and version: " + testTypeName);
        }

        String testName = config.get(LauncherProperties.SERVER_TEST_NAME, testPath.getFileName().toString());
        String testBuild = config.get(LauncherProperties.SERVER_TEST_BUILD, "latest");
        return new Server(
                testPath,
                testName,
                new ServerVersion(testType, testVersion, testBuild),
                0
        );
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Iterable<ModdableGame> getGames() {
        return (Iterable) getContents();
    }

    @Override
    public boolean providesOnlyServers() {
        return true;
    }

    @Override
    public void download(Launcher launcher, VersionId version, String... args) throws CommandException {
        ServerType serverType = getServerType(version.getPlatform().getName());
        if (serverType == null) {
            throw new CommandException("Server type not found: " + version.getPlatform().getName());
        }

        try {
            add(launcher, serverType, null, version.getName(), version.getBuild(), args);
        } catch (IOException e) {
            log.error("Failed to download " + version, e);
            throw new CommandException(e.getMessage());
        }
    }

}

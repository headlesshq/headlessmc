package io.github.headlesshq.headlessmc.launcher.launch;

import lombok.CustomLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import io.github.headlesshq.headlessmc.api.command.line.Progressbar;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.launcher.auth.AuthException;
import io.github.headlesshq.headlessmc.launcher.download.AssetsDownloader;
import io.github.headlesshq.headlessmc.launcher.download.DownloadService;
import io.github.headlesshq.headlessmc.launcher.download.LibraryDownloader;
import io.github.headlesshq.headlessmc.launcher.files.FileManager;
import io.github.headlesshq.headlessmc.launcher.files.LauncherConfig;
import io.github.headlesshq.headlessmc.launcher.instrumentation.Instrumentation;
import io.github.headlesshq.headlessmc.launcher.instrumentation.InstrumentationHelper;
import io.github.headlesshq.headlessmc.launcher.instrumentation.Target;
import io.github.headlesshq.headlessmc.launcher.modlauncher.Modlauncher;
import io.github.headlesshq.headlessmc.launcher.specifics.VersionSpecificException;
import io.github.headlesshq.headlessmc.launcher.specifics.VersionSpecificModManager;
import io.github.headlesshq.headlessmc.launcher.specifics.VersionSpecificModRepository;
import io.github.headlesshq.headlessmc.launcher.specifics.VersionSpecificMods;
import io.github.headlesshq.headlessmc.launcher.version.Features;
import io.github.headlesshq.headlessmc.launcher.version.Rule;
import io.github.headlesshq.headlessmc.launcher.version.Version;
import io.github.headlesshq.headlessmc.launcher.version.VersionExecutable;
import io.github.headlesshq.headlessmc.os.OS;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;

@Getter
@CustomLog
@RequiredArgsConstructor
public class ProcessFactory {
    private final DownloadService downloadService;
    private final LauncherConfig config;
    private final OS os;

    public @Nullable Process run(LaunchOptions options) throws LaunchException, AuthException, IOException {
        val instrumentation = InstrumentationHelper.create(options);
        return run(options, instrumentation);
    }

    public @Nullable Process run(LaunchOptions options, Instrumentation instrumentation) throws IOException, LaunchException, AuthException {
        val launcher = options.getLauncher();

        log.debug("Creating version merger");
        val version = new VersionMerger(options.getVersion());
        if (version.getArguments() == null) {
            throw new LaunchException(
                version.getName() + ": Version file and its parents" +
                    " didn't contain arguments.");
        }

        log.debug("Creating extraction directory");
        val natives = options.getFiles().createRelative("extracted");
        val targets = processLibraries(options, version, natives);
        addGameJar(version, targets);

        List<String> classpath = instrumentation.instrument(targets);
        if (options.isRuntime()) {
            moveRuntimeJarToFirstPlace(classpath);
        }

        log.debug("Building command");
        val commandBuilder = configureCommandBuilder(options, version, classpath, natives).build();

        val command = commandBuilder.build();
        downloadAssets(options, config.getMcFiles(), version);
        debugCommand(command, commandBuilder);

        val dir = new File(launcher.getConfig().get(LauncherProperties.GAME_DIR, launcher.getGameDir(version).getPath()));
        log.info("Game will run in " + dir);
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        autoDownloadSpecifics(options, version, dir.toPath());
        if (options.isPrepare()) {
            return null;
        }

        if (options.isCloseCommandLine()) {
            launcher.getCommandLine().close();
        }

        System.gc(); // suggest JVM to clean up before running mc
        if (options.isInMemory()) {
            inMemoryLaunch(new InMemoryLauncher(options, commandBuilder, version, launcher.getJavaService().getCurrent()));
            return null;
        }

        return this.run(new ProcessBuilder()
            .command(command)
            .directory(dir)
            .redirectError(options.isNoOut()
                               ? ProcessBuilder.Redirect.PIPE
                               : ProcessBuilder.Redirect.INHERIT)
            .redirectOutput(options.isNoOut()
                                ? ProcessBuilder.Redirect.PIPE
                                : ProcessBuilder.Redirect.INHERIT)
            .redirectInput(options.isNoIn()
                               ? ProcessBuilder.Redirect.PIPE
                               : ProcessBuilder.Redirect.INHERIT));
    }

    protected JavaLaunchCommandBuilder.JavaLaunchCommandBuilderBuilder configureCommandBuilder(
            LaunchOptions options, Version version, List<String> classpath, FileManager natives) {
        return JavaLaunchCommandBuilder
                .builder()
                .account(options.getAccount())
                .classpath(classpath)
                .os(os)
                .jvmArgs(options.getAdditionalJvmArgs())
                .gameArgs(options.getAdditionalGameArgs())
                .natives(natives.getBase().getAbsolutePath())
                .runtime(options.isRuntime())
                .version(version)
                .launcher(options.getLauncher())
                .inMemory(options.isInMemory())
                .lwjgl(options.isLwjgl());
    }

    protected void moveRuntimeJarToFirstPlace(List<String> classpath) {
        String runtimeJar = null;
        for (String path : classpath) {
            if (path.endsWith(InstrumentationHelper.RUNTIME_JAR)) {
                runtimeJar = path;
                classpath.remove(path);
                break;
            }
        }

        if (runtimeJar == null) {
            throw new IllegalStateException("Failed to find RuntimeJar in classpath " + classpath);
        }
        // add RuntimeJar as the first jar on the classpath
        // this makes java look it up for libraries first
        // really important because forge provides an incompatible version of JLine.
        // TODO: this works, but is it really something we want to trust?
        //  bring over the VersionAgnosticJLineCommandLineReader from hmc-specifics?
        classpath.add(0, runtimeJar);
    }

    protected void inMemoryLaunch(InMemoryLauncher inMemoryLauncher) throws LaunchException, AuthException, IOException {
        inMemoryLauncher.launch();
    }

    protected void addGameJar(Version version, List<Target> targets) throws IOException {
        File gameJar = new File(version.getFolder(), version.getName() + ".jar");
        log.debug("GameJar: " + gameJar.getAbsolutePath());
        if (!gameJar.exists() || !checkZipIntact(gameJar) && gameJar.delete()) {
            LibraryDownloader downloader = new LibraryDownloader(downloadService, config.getConfig(), os);
            VersionExecutable clientDownload = version.getClientDownload();
            if (clientDownload == null) {
                throw new IOException("Failed find client download in version " + version.getName());
            }

            log.info("Downloading " + version.getName() + " from " + clientDownload.getUrl());
            downloader.download(clientDownload.getUrl(), gameJar.toPath().toAbsolutePath(), clientDownload.getSha1(), clientDownload.getSize());
        }

        targets.add(new Target(true, gameJar.getAbsolutePath()));
        log.debug("Processed GameJar");
    }

    protected List<Target> processLibraries(LaunchOptions options, Version version, FileManager dlls) throws IOException {
        log.debug("Processing libraries...");
        // TODO: proper features
        val features = Features.EMPTY;
        val targets = new ArrayList<Target>(version.getLibraries().size());
        Set<String> libPaths = new HashSet<>();
        LibraryDownloader libraryDownloader = new LibraryDownloader(downloadService, config.getConfig(), os);
        int librariesTODownload = 0;
        for (val library : version.getLibraries()) {
            if (library.getRule().apply(os, features) == Rule.Action.ALLOW) {
                String libPath = library.getPath(os);
                if (!libPaths.add(libPath)) {
                    continue;
                }

                val path = config.getMcFiles().getDir("libraries").toPath().resolve(libPath);
                if ((library.getSha1() != null || library.getSize() != null)
                        && config.getConfig().getConfig().get(LauncherProperties.LIBRARIES_CHECK_FILE_HASH, false)
                        && Files.exists(path)
                        && !downloadService.getChecksumService().checkIntegrity(path, library.getSize(), library.getSha1())) {
                    log.warn("Library " + libPath + " failed integrity check, deleting...");
                    Files.delete(path);
                }

                if (!Files.exists(path)) {
                    librariesTODownload++;
                }
            }
        }

        libPaths.clear();
        try (Progressbar progressbar = librariesTODownload == 0
                ? Progressbar.dummy()
                : options.getLauncher().getCommandLine().displayProgressBar(new Progressbar.Configuration("Downloading Libraries", librariesTODownload))) {
            libraryDownloader.setShouldLog(progressbar.isDummy());
            for (val library : version.getLibraries()) {
                if (library.getRule().apply(os, features) == Rule.Action.ALLOW) {
                    log.debug("Checking: " + library);
                    String libPath = library.getPath(os);
                    if (!libPaths.add(libPath)) {
                        continue;
                    }

                    val path = config.getMcFiles().getDir("libraries").toPath().resolve(libPath);
                    if (!Files.exists(path)) {
                        libraryDownloader.download(library, path);
                        progressbar.step();
                    }

                    String absolutePath = path.toAbsolutePath().toString();
                    library.getExtractor().extract(absolutePath, dlls);
                    if (!library.isNativeLibrary()) {
                        targets.add(new Target(false, absolutePath));
                    }
                } else {
                    log.debug("Ignoring: " + library.getName());
                }
            }
        }

        log.debug("Finished processing libraries");
        return targets;
    }

    protected boolean checkZipIntact(File file) {
        val name = file.getName();
        boolean result = true;
        if (name.endsWith(".jar") || name.endsWith(".zip")) {
            try {
                val zipFile = new ZipFile(file);
                zipFile.close();
            } catch (IOException e) {
                log.error("Couldn't read " + name + " : " + e.getMessage());
                result = false;
            }
        }

        return result;
    }

    protected Process run(ProcessBuilder builder) throws IOException {
        return builder.start();
    }

    protected void downloadAssets(LaunchOptions options, FileManager files, Version version) throws IOException {
        log.debug("Downloading Assets");
        new AssetsDownloader(options.getLauncher().getCommandLine(), downloadService, config.getConfig(), files, version.getAssetsUrl(), version.getAssets()).download();
    }

    /**
     * @return a FileManager representing the .minecraft directory.
     */
    public FileManager getFiles() {
        return config.getMcFiles();
    }

    private void debugCommand(List<String> command, JavaLaunchCommandBuilder commandBuilder) {
        StringBuilder commandDebugBuilder = new StringBuilder();
        if (!command.isEmpty()) {
            commandDebugBuilder.append("\"").append(command.get(0)).append("\" "); // escape java path
        }

        for (int i = 1; i < command.size(); i++) {
            if (commandBuilder.getAccount().getToken().equals(command.get(i))) {
                commandDebugBuilder.append("********").append((i == command.size() - 1) ? "" : " ");
            } else {
                commandDebugBuilder.append(command.get(i)).append((i == command.size() - 1) ? "" : " ");
            }
        }

        log.debug(commandDebugBuilder.toString());
    }

    private void autoDownloadSpecifics(LaunchOptions options, Version version, Path gameDir) throws IOException {
        if (options.isSpecifics()) {
            if (Modlauncher.getFromVersionName(version.getName()) == null) {
                return;
            }

            VersionSpecificModRepository repo = VersionSpecificMods.HMC_SPECIFICS;
            VersionSpecificModManager modManager = options.getLauncher().getVersionSpecificModManager();
            log.info("Automatically downloading Hmc-Specifics " + repo.getVersion(downloadService));
            try {
                modManager.download(version, repo);
                modManager.deleteSpecificsOfOtherVersions(version, repo, gameDir.resolve("mods"));
                modManager.install(version, repo, gameDir.resolve("mods"));
            } catch (VersionSpecificException e) {
                throw new IOException(e);
            }
        }
    }

}

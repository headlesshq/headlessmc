package io.github.headlesshq.headlessmc.launcher.command.forge;

import io.github.headlesshq.headlessmc.launcher.version.Version;
import lombok.CustomLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.files.FileManager;
import io.github.headlesshq.headlessmc.launcher.instrumentation.ResourceExtractor;
import io.github.headlesshq.headlessmc.launcher.launch.SimpleInMemoryLauncher;
import io.github.headlesshq.headlessmc.launcher.util.JsonUtil;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Getter
@CustomLog
@RequiredArgsConstructor
public class ForgeInstaller {
    private static final String FORGE_CLI = "forge-cli.jar";

    private final SimpleInMemoryLauncher inMemoryLauncher = new ForgeInMemoryLauncher();
    private final ForgeRepoFormat repoFormat;
    private final Launcher launcher;
    private final String forgeName;
    private final String baseUrl;

    public void install(Version vanillaVersion, ForgeVersion version, FileManager fileManager, boolean inMemory) throws IOException {
        install(vanillaVersion, version, fileManager, launcher.getMcFiles(), inMemory, false);
    }

    public void installServer(Version vanillaVersion, ForgeVersion version, FileManager fileManager, @Nullable String dir, boolean inMemory) throws IOException {
        FileManager installDir = dir == null ? fileManager : new FileManager(dir);
        install(vanillaVersion, version, fileManager, installDir, inMemory, true);
    }

    private void install(Version vanillaVersion, ForgeVersion version, FileManager fileManager, FileManager installDir, boolean inMemory, boolean server) throws IOException {
        String fileName = repoFormat.getFileName(version);
        File installer = fileManager.create(fileName);
        downloadInstaller(version, installer);

        int bestJavaVersion = vanillaVersion == null ? 8 : (vanillaVersion.getJava() == null ? 21 : vanillaVersion.getJava());
        Java java = inMemory ? launcher.getJavaService().getCurrent() : launcher.getJavaService().findBestVersion(launcher, bestJavaVersion, true);
        if (java == null) {
            java = launcher.getJavaService().findBestVersion(launcher, bestJavaVersion);
            if (java == null) {
                throw new IOException("No Java version found! Please configure hmc.java.versions.");
            }
        }

        if (server) {
            List<String> command = getCommand(java, installDir.getBase(), installer, installer, inMemory, true);
            launch(version, installer.toPath(), installDir.getBase().toPath(), command, inMemory);
        } else {
            File cli = new ResourceExtractor(fileManager, FORGE_CLI).extract();
            List<String> command = getCommand(java, installDir.getBase(), cli, installer, inMemory, false);

            ensureJsonExists("launcher_profiles.json", installDir);
            ensureJsonExists("launcher_profiles_microsoft_store.json", installDir);

            launch(version, cli.toPath(), fileManager.getBase().toPath(), command, inMemory);
        }
    }

    private void launch(ForgeVersion version, Path jar, Path installDir, List<String> command, boolean inMemory) throws IOException {
        if (inMemory) {
            try {
                // Our ForgeCLI does the following:
                // try (URLClassLoader ucl = new InstallerClassLoader(new URL[]{
                //                Main.class.getProtectionDomain().getCodeSource().getLocation(),
                // When running inside an IDE Main.class.getProtectionDomain().getCodeSource().getLocation() is null!
                inMemoryLauncher.simpleLaunch(new URL[] { jar.toFile().toURI().toURL() }, inMemoryLauncher.getMainClassFromJar(jar.toFile()), command);
                launcher.log(String.format("%s %s installed successfully!", forgeName, version.getFullName()));
            } catch (Throwable t) {
                throw new IOException(t);
            }
        } else {
            Process process = new ProcessBuilder()
                    .directory(installDir.toFile())
                    .command(command)
                    .inheritIO()
                    .start();
            try {
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new IOException(String.format("Failed to install Forge %s, exitCode: %d", version.getFullName(), exitCode));
                } else {
                    launcher.log(String.format("%s %s installed successfully!", forgeName, version.getFullName()));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread has been interrupted!");
                throw new IllegalStateException(e);
            }
        }
    }

    protected List<String> getCommand(Java java, File installDir, File jar, File fml, boolean inMemory, boolean server) {
        List<String> command = new ArrayList<>();
        if (!inMemory) {
            command.add(java.getExecutable());
            command.add("-jar");
            command.add(jar.getAbsolutePath());
        }

        if (server) {
            command.add("--installServer");
            command.add(installDir.getAbsolutePath());
        } else {
            command.add("--installer");
            command.add(fml.getAbsolutePath());
            command.add("--target");
            command.add(installDir.getAbsolutePath());
        }

        return command;
    }

    protected void downloadInstaller(ForgeVersion version, File file) throws IOException {
        String url = repoFormat.getUrl(baseUrl, version);
        log.debug("Downloading Installer from " + url);
        try {
            launcher.getDownloadService().download(url, file.toPath());
        } catch (IOException e) {
            log.debug("Failed to download Forge from " + url + ": " + e.getMessage());
            url = baseUrl + version.getFullName() + "-" + version.getVersion()
                + "/forge-" + version.getFullName() + "-" + version.getVersion()
                + "-installer.jar";
            log.debug("Downloading from forge from " + url);
            launcher.getDownloadService().download(url, file.toPath());
        }
    }

    private void ensureJsonExists(String name, FileManager mcFiles) throws IOException {
        File file = mcFiles.create(name);
        try {
            if (!JsonUtil.fromFile(file).isJsonObject()) {
                throw new IOException("Not a JsonObject!");
            }
        } catch (IOException ioException) {
            log.debug("Writing in " + name);
            Files.write(file.toPath(), "{\"profiles\": {}}".getBytes(StandardCharsets.UTF_8));
        }
    }

    @Deprecated
    public void install(ForgeVersion version, FileManager fileManager, boolean inMemory) throws IOException {
        install(null, version, fileManager, launcher.getMcFiles(), inMemory, false);
    }

    @Deprecated
    public void installServer(ForgeVersion version, FileManager fileManager, @Nullable String dir, boolean inMemory) throws IOException {
        FileManager installDir = dir == null ? fileManager : new FileManager(dir);
        install(null, version, fileManager, installDir, inMemory, true);
    }

    @Deprecated
    public void install(ForgeVersion version, FileManager fileManager, FileManager installDir, boolean inMemory, boolean server) throws IOException {
        install(null, version, fileManager, installDir, inMemory, server);
    }

}

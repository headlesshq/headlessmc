package io.github.headlesshq.headlessmc.launcher.command;

import lombok.CustomLog;
import lombok.Getter;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.CommandUtil;
import io.github.headlesshq.headlessmc.api.command.ParseUtil;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.launcher.command.download.AbstractDownloadingVersionCommand;
import io.github.headlesshq.headlessmc.launcher.command.download.ModLauncherCommand;
import io.github.headlesshq.headlessmc.launcher.files.FileManager;
import io.github.headlesshq.headlessmc.launcher.launch.SimpleInMemoryLauncher;
import io.github.headlesshq.headlessmc.launcher.launch.SystemPropertyHelper;
import io.github.headlesshq.headlessmc.launcher.version.Version;
import io.github.headlesshq.headlessmc.launcher.version.VersionUtil;
import io.github.headlesshq.headlessmc.launcher.version.family.FamilyUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

@Getter
@CustomLog
public class FabricCommand extends AbstractDownloadingVersionCommand implements ModLauncherCommand {
    private static final String LEGACY = "https://maven.legacyfabric.net/net/legacyfabric/fabric-installer/1.0.0/fabric-installer-1.0.0.jar";
    private static final String URL = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.3/fabric-installer-1.0.3.jar";

    private final SimpleInMemoryLauncher inMemoryLauncher = new SimpleInMemoryLauncher();

    public FabricCommand(Launcher ctx) {
        super(ctx, "fabric", "Downloads Fabric for the specified version.");
        args.put("<version>", "The version to download.");
        args.put("--jvm", "Jvm args for the Fabric Installer.");
        args.put("--java", "Java version to use (e.g. 8, 17).");
        args.put("--uid", "Specify a specific Fabric version.");
        args.put("-inmemory", "If you want to run the installer inside this JVM.");
    }

    @Override
    public void execute(Version ver, String... args) throws CommandException {
        ctx.log("Installing Fabric for version " + ver.getName() + "...");
        FileManager tempFiles = ctx.getFileManager().createRelative(UUID.randomUUID().toString());
        File jar = tempFiles.create("fabric-installer.jar");
        Version vanilla = FamilyUtil.getOldestParent(ver);
        String defaultUrl = URL;
        if (VersionUtil.isOlderThanSafe(vanilla.getName(), "1.14") && !CommandUtil.hasFlag("-forcenew", args) || CommandUtil.hasFlag("-legacy", args)) {
            ctx.log("Using Legacy Fabric...");
            defaultUrl = LEGACY;
        }

        String url = ctx.getConfig().get(LauncherProperties.FABRIC_URL, defaultUrl);
        try {
            downloadInstaller(url, jar);
            install(ver, jar, args);
        } finally {
            try {
                log.debug("Deleting: " + jar.getAbsolutePath());
                ctx.getFileManager().delete(tempFiles.getBase());
            } catch (IOException e) {
                log.error(
                        "Failed to delete: " + jar.getAbsolutePath() + " : "
                                + e.getMessage());
            }
        }

        ctx.log("Installed Fabric for: " + ver.getName() + " successfully!");
    }

    private void downloadInstaller(String url, File jar) throws CommandException {
        try {
            ctx.getDownloadService().download(url, jar.toPath());
        } catch (IOException e) {
            throw new CommandException(
                    "Couldn't download Fabric installer from " + url + " to "
                            + jar.getAbsolutePath() + " : " + e.getMessage());
        }
    }

    private void install(Version version, File jarFile, String... args) throws CommandException {
        int bestVersion = version.getJava() == null ? 21 : version.getJava();
        String javaVersion = CommandUtil.getOption("--java", args);
        if (javaVersion != null) {
            bestVersion = ParseUtil.parseI(javaVersion);
        }

        boolean inMemory = CommandUtil.hasFlag("-inmemory", args) || ctx.getConfig().get(LauncherProperties.ALWAYS_IN_MEMORY, false);
        Java java = inMemory ? ctx.getJavaService().getCurrent() : ctx.getJavaService().findBestVersion(ctx, bestVersion, true);
        if (java == null) {
            java = ctx.getJavaService().findBestVersion(ctx, bestVersion);
            if (java == null) {
                throw new CommandException("No Java version found! Please configure hmc.java.versions.");
            }
        }

        String jvmArgs = CommandUtil.getOption("--jvm", args);
        List<String> jvm = Collections.emptyList();
        if (jvmArgs != null) {
            jvm = new ArrayList<>(Arrays.asList(CommandUtil.split(jvmArgs)));
        }

        Properties properties = System.getProperties();
        Properties systemPropertiesBefore = new Properties();
        for (Object key : properties.keySet()) {
            systemPropertiesBefore.put(key, properties.getProperty(key.toString()));
        }

        List<String> command = getCommand(version, java, jarFile, jvm, inMemory, args);

        try {
            log.debug("Launching Fabric-Installer for command: " + command);
            if (inMemory) {
                try {
                    inMemoryLauncher.simpleLaunch(new URL[]{jarFile.toURI().toURL()}, inMemoryLauncher.getMainClassFromJar(jarFile), command);
                    log.debug("Fabric-Installer finished.");
                } finally {
                    // restore old system properties
                    System.getProperties().clear();
                    System.getProperties().putAll(systemPropertiesBefore);
                }
            } else {
                Process process = new ProcessBuilder()
                        .directory(ctx.getMcFiles().getBase())
                        .command(command)
                        .inheritIO()
                        .start();

                int exitCode = process.waitFor();
                log.debug("Fabric-Installer quit with exit-code: " + exitCode);
                if (exitCode != 0) {
                    throw new CommandException(
                            "Fabric installer exit code: " + exitCode
                                    + ". Failed to install fabric for version "
                                    + version.getName());
                }
            }

            ctx.getVersionService().refresh();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread got interrupted!");
            throw new IllegalStateException("Thread got interrupted!");
        } catch (Throwable throwable) {
            String message = "Fabric installation failed: " + throwable.getMessage();
            log.error(message);
            throw new CommandException(message);
        }
    }

    protected List<String> getCommand(Version version, Java java, File jar, List<String> jvm, boolean inMemory, String... args) {
        List<String> command = new ArrayList<>();
        if (inMemory) {
            for (String jvmArg : jvm) {
                if (SystemPropertyHelper.isSystemProperty(jvmArg)) {
                    String[] keyValue = SystemPropertyHelper.splitSystemProperty(jvmArg);
                    System.setProperty(keyValue[0], keyValue[1]);
                }
            }
        } else {
            command.add(java.getExecutable());
            command.addAll(jvm);
            command.add("-jar");
            command.add(jar.getAbsolutePath());
        }

        if (CommandUtil.hasFlag("-server", args)) {
            command.add("server");
        } else {
            command.add("client");
            command.add("-noprofile");
        }

        command.add("-mcversion");
        command.add(version.getName());
        String uid = CommandUtil.getOption("--uid", args);
        if (uid != null) {
            log.info("Adding -loader " + uid);
            command.add("-loader");
            command.add(uid);
        }

        command.add("-dir");
        String dir = CommandUtil.getOption("--dir", args);
        if (dir == null) {
            dir = ctx.getMcFiles().getBase().toPath().toAbsolutePath().toString();
        }

        command.add(dir);
        return command;
    }

}

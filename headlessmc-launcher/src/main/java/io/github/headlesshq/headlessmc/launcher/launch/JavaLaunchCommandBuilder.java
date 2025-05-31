package io.github.headlesshq.headlessmc.launcher.launch;

import io.github.headlesshq.headlessmc.api.command.CommandLineReader;
import io.github.headlesshq.headlessmc.api.config.HmcProperties;
import io.github.headlesshq.headlessmc.api.settings.Config;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.jline.JLineCommandLineReader;
import io.github.headlesshq.headlessmc.jline.JLineProperties;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.launcher.auth.AuthException;
import io.github.headlesshq.headlessmc.launcher.auth.LaunchAccount;
import io.github.headlesshq.headlessmc.launcher.version.Features;
import io.github.headlesshq.headlessmc.launcher.version.Logging;
import io.github.headlesshq.headlessmc.launcher.version.Version;
import io.github.headlesshq.headlessmc.os.OS;
import lombok.Builder;
import lombok.CustomLog;
import lombok.Getter;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Builder
@CustomLog
public class JavaLaunchCommandBuilder {
    private static final String RT_MAIN = "io.github.headlesshq.headlessmc.runtime.Main";

    private final LaunchAccount account;
    private final List<String> classpath;
    private final List<String> jvmArgs;
    private final List<String> gameArgs;
    private final Launcher launcher;
    private final Version version;
    private final String natives;
    private final boolean runtime;
    private final boolean lwjgl;
    private final boolean inMemory;
    private final OS os;

    public List<String> build() throws LaunchException, AuthException {
        Config config = launcher.getConfig();
        Java java;
        try {
            java = inMemory ? launcher.getJavaService().getCurrent() : launcher.getJavaService().findBestVersion(launcher, version.getJava());
        } catch (IOError e) {
            throw new LaunchException("Could not find Java " + version.getJava(), e);
        }

        if (inMemory) {
            if (java == null || java.getVersion() != version.getJava()) {
                if (launcher.getConfig().get(LauncherProperties.IN_MEMORY_REQUIRE_CORRECT_JAVA, true)) {
                    throw new LaunchException("Running in memory with " + (java == null ? "unknown" : "") + " java version "
                                                  + (java == null ? "" : (java.getVersion() + "but "))
                                                  + "minecraft needs "
                                                  + version.getJava());
                }

                log.warning("Running in memory with "+ (java == null ? "unknown" : "") + " java version "
                                + (java == null ? "" : (java.getVersion() + "but "))
                                + "minecraft needs "
                                + version.getJava());
            } else {
                log.info("Running with Minecraft in memory in this JVM.");
            }

            if (java == null) {
                java = new Java("unknown", version.getJava());
            }
        } else if (java == null) {
            throw new LaunchException("Couldn't find Java version for "
                                          + version.getName()
                                          + ", requires Java "
                                          + version.getJava());
        }

        List<String> result = new ArrayList<>();
        result.add(java.getExecutable());
        result.addAll(Arrays.asList(config.get(LauncherProperties.JVM_ARGS, new String[0])));
        if (config.get(LauncherProperties.SET_LIBRARY_DIR, true)) {
            result.add(SystemPropertyHelper.toSystemProperty("libraryDirectory", launcher.getMcFiles().getDir("libraries").getAbsolutePath()));
        }

        result.addAll(jvmArgs);

        if (runtime
            && java.getVersion() > 8
            && config.get(HmcProperties.DEENCAPSULATE, true)) {
            log.info("Java version > 8 detected, deencapsulating!");
            result.add(SystemPropertyHelper.toSystemProperty(HmcProperties.DEENCAPSULATE.getName(), "true"));
        }

        CommandLineReader commandLineReader = launcher.getCommandLine().getCommandLineReader();
        if (runtime && commandLineReader instanceof JLineCommandLineReader && ((JLineCommandLineReader) commandLineReader).isDumb()
                || config.get(JLineProperties.PROPAGATE_ENABLED, true) && !config.get(JLineProperties.ENABLED, true)) {
            result.add(SystemPropertyHelper.toSystemProperty(JLineProperties.ENABLED.getName(), "false"));
        }

        if (lwjgl && config.get(LauncherProperties.JOML_NO_UNSAFE, true)) {
            result.add("-Djoml.nounsafe=true");
        }

        if (inMemory) {
            result.add(SystemPropertyHelper.toSystemProperty(LauncherProperties.IN_MEMORY.getName(), "true"));
        }

        // we generally do not download logging, because it seems to log xml on the console... TODO: why?
        // instead the log4j patch is on by default
        // also old version logging, like 1.8.9 does not contain the {nolookups}, so they are unsafe anyways
        if (launcher.getConfig().get(LauncherProperties.INSTALL_LOGGING, false)) {
            Logging logging = version.getLogging();
            if (logging == null) {
                throw new IllegalArgumentException("Version " + version + " has no logging configured!");
            }

            File file = launcher.getMcFiles().get(false, true, "logging", logging.getFile().getId());
            installLogging(logging, file);
            result.add(logging.getArgument().replace("${path}", file.getAbsolutePath()));
        }

        result.add("-Djava.library.path=" + natives);
        result.add("-cp");
        result.add(String.join("" + File.pathSeparatorChar, classpath) + config.get(LauncherProperties.CLASS_PATH, ""));

        ArgumentAdapter adapter = ArgumentAdapterHelper.create(launcher, version, natives, account);
        result.addAll(adapter.build(os, Features.EMPTY, "jvm"));
        addIgnoreList(result);
        getActualMainClass(result);
        result.addAll(adapter.build(os, Features.EMPTY, "game"));
        result.addAll(Arrays.asList(config.get(LauncherProperties.GAME_ARGS, new String[0])));
        result.addAll(gameArgs);
        return result;
    }

    // TODO: not the correct location but I dont care
    private void installLogging(Logging logging, File file) throws LaunchException {
        Logging.File loggingFile = logging.getFile();
        try {
            if (!file.exists() || !launcher.getSha1Service().checkIntegrity(file.toPath(), loggingFile.getSize(), loggingFile.getSha1())) {
                log.info("Downloading logging file: " + loggingFile.getUrl());
                launcher.getDownloadService().download(loggingFile.getUrl(), file.toPath(), loggingFile.getSize(), loggingFile.getSha1());
                if (!launcher.getSha1Service().checkIntegrity(file.toPath(), loggingFile.getSize(), loggingFile.getSha1())) {
                    throw new LaunchException("Logging file failed integrity check! (" + logging + ")");
                }
            }
        } catch (IOException e) {
            throw new LaunchException("Failed to install logging " + logging, e);
        }
    }

    public String getActualMainClass(List<String> result) {
        String mainClass = version.getMainClass();
        if (runtime) {
            result.add(SystemPropertyHelper.toSystemProperty(HmcProperties.MAIN.getName(), version.getMainClass()));
            mainClass = RT_MAIN;
        }

        mainClass = launcher.getConfig().get(LauncherProperties.CUSTOM_MAIN_CLASS, mainClass);
        result.add(mainClass);
        return mainClass;
    }

    // here to make the javadoc happy
    public static class JavaLaunchCommandBuilderBuilder {

    }

}

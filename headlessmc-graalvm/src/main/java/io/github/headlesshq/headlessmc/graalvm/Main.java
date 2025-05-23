package io.github.headlesshq.headlessmc.graalvm;

import lombok.CustomLog;
import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.command.ProgressBarProvider;
import io.github.headlesshq.headlessmc.api.process.StdIO;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.download.JavaDownloadRequest;
import io.github.headlesshq.headlessmc.java.download.JavaDownloaderManager;
import io.github.headlesshq.headlessmc.jline.JLineProperties;
import io.github.headlesshq.headlessmc.jline.JlineProgressbarProvider;
import io.github.headlesshq.headlessmc.launcher.Service;
import io.github.headlesshq.headlessmc.launcher.files.AutoConfiguration;
import io.github.headlesshq.headlessmc.launcher.files.ConfigService;
import io.github.headlesshq.headlessmc.launcher.files.FileManager;
import io.github.headlesshq.headlessmc.launcher.java.JavaService;
import io.github.headlesshq.headlessmc.logging.LoggingService;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.os.OSFactory;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@CustomLog
public class Main {
    private static final Path HEADLESSMC_PATH = Paths.get(HeadlessMc.NAME);

    public static void main(String[] args) throws IOException {
        LoggingService service = new LoggingService();
        service.setFileHandler(false);
        service.init();

        if (args.length > 0
                && (args[0].equalsIgnoreCase("version")
                    || args[0].equalsIgnoreCase("-version")
                    || args[0].equalsIgnoreCase("--version"))) {
            new StdIO().getOut().get().println(HeadlessMc.NAME + " - " + HeadlessMc.VERSION);
            return;
        }

        FileManager fileManager = new FileManager(HEADLESSMC_PATH.toAbsolutePath().toString());
        AutoConfiguration.runAutoConfiguration(fileManager);
        ConfigService configs = Service.refresh(new ConfigService(fileManager));
        OS os = OSFactory.detect(configs.getConfig());

        Java java = findJava(configs, os);
        runHmcOn(java, args);
    }

    private static void runHmcOn(Java java, String[] args) throws IOException {
        Path launcherWrapper = HEADLESSMC_PATH.resolve("headlessmc-launcher-wrapper.jar");
        try (InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("headlessmc/headlessmc-launcher-wrapper.jar");
             OutputStream outputStream = Files.newOutputStream(launcherWrapper)) {
            Objects.requireNonNull(inputStream, "Failed to find headlessmc-launcher-wrapper resource").transferTo(outputStream);
        }

        RuntimeMXBean mx = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = new ArrayList<>();
        arguments.add(java.getExecutable());
        arguments.addAll(mx.getInputArguments());
        arguments.add("-jar");
        arguments.add(launcherWrapper.toAbsolutePath().toString());
        if (args.length > 0 && !args[0].equalsIgnoreCase("cli") && !args[0].equalsIgnoreCase("--command")) {
            arguments.add("--command");
        }

        arguments.addAll(Arrays.asList(args));

        Process process = new ProcessBuilder(arguments)
                .inheritIO()
                .directory(null) // use current directory
                .start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (process.isAlive()) {
                process.destroy();
            }
        }));

        try {
            System.exit(process.waitFor());
        } catch (InterruptedException e) {
            if (process.isAlive()) {
                process.destroy();
            }

            throw new IllegalStateException(e);
        }
    }

    private static Java findJava(ConfigService configs, OS os) throws IOException {
        int requestedVersion = configs.getConfig().get(GraalProperties.JAVA_VERSION, -1L).intValue();
        String distribution = configs.getConfig().get(GraalProperties.JAVA_DISTRIBUTION, JavaDownloaderManager.DEFAULT_DISTRIBUTION);
        boolean forceDownload = configs.getConfig().get(GraalProperties.FORCE_DOWNLOAD, false);

        JavaService javaService = Service.refresh(new JavaService(configs, os));
        Java java = findJava(javaService, requestedVersion);
        if (java == null || forceDownload) {
            int versionToDownload = requestedVersion == -1 ? 21 : requestedVersion;
            JavaDownloaderManager downloaderManager = JavaDownloaderManager.getDefault();

            ProgressBarProvider progressBarProvider = ProgressBarProvider.dummy();
            boolean progressBarEnabled = configs.getConfig().get(JLineProperties.ENABLE_PROGRESS_BAR, true);
            if (progressBarEnabled) {
                progressBarProvider = new JlineProgressbarProvider(configs);
            } else {
                log.info("Downloading Java " + requestedVersion + " (" + distribution + ")");
            }

            JavaDownloadRequest request = new JavaDownloadRequest(
                    new Java11DownloadClient(),
                    progressBarProvider,
                    versionToDownload,
                    distribution,
                    os,
                    configs.getConfig().get(GraalProperties.JDK, false)
            );

            downloaderManager.download(HEADLESSMC_PATH.resolve("java"), request);
            javaService.refreshHeadlessMcJavaVersions();
            java = findJava(javaService, requestedVersion);
            if (java == null) {
                throw new IllegalStateException("Failed to install Java " + requestedVersion + " (" + distribution + ")");
            }
        }

        return java;
    }

    private static @Nullable Java findJava(JavaService javaService, int requestedVersion) {
        List<Java> javas = new ArrayList<>();
        javaService.forEach(javas::add);
        javas.removeIf(java -> java.getVersion() < 8);
        javas.removeIf(Java::isInvalid);
        Collections.sort(javas);

        return requestedVersion == -1
                ? javas.stream().reduce((first, second) -> second).orElse(null) // find last, with the newest java version
                : javas.stream().filter(j -> j.getVersion() == requestedVersion).findFirst().orElse(null);
    }

}

package io.github.headlesshq.headlessmc.launcher.server;

import lombok.CustomLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.CommandUtil;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.launcher.auth.AuthException;
import io.github.headlesshq.headlessmc.launcher.auth.LaunchAccount;
import io.github.headlesshq.headlessmc.launcher.command.download.VersionInfo;
import io.github.headlesshq.headlessmc.launcher.command.download.VersionInfoUtil;
import io.github.headlesshq.headlessmc.launcher.files.FileManager;
import io.github.headlesshq.headlessmc.launcher.launch.LaunchException;
import io.github.headlesshq.headlessmc.launcher.launch.LaunchOptions;
import io.github.headlesshq.headlessmc.launcher.util.IOUtil;
import io.github.headlesshq.headlessmc.launcher.version.Version;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.headlesshq.headlessmc.launcher.LauncherProperties.ALWAYS_IN_MEMORY;

@Getter
@Setter
@CustomLog
@RequiredArgsConstructor
public class ServerLauncher {
    private final Launcher launcher;
    private final Server server;
    private final String[] args;

    private boolean prepare = false;
    private boolean quit = false;
    private @Nullable String eula;

    public String readEula() throws IOException {
        try (BufferedReader br = IOUtil.reader(Files.newInputStream(server.getEula(isInMemory())))) {
            eula = IOUtil.read(br, true);
            return eula;
        }
    }

    public void acceptEula() throws IOException {
        if (eula == null) {
            eula = readEula();
        }

        eula = eula.replace("eula=false", "eula=true");
        Files.write(server.getEula(isInMemory()), eula.getBytes(StandardCharsets.UTF_8));
    }

    // TODO: check if necessary, or if we could also just create a file containing eula=true
    public void eulaLaunch() throws CommandException, LaunchException, IOException {
        if (!server.hasEula(isInMemory())) {
            if (launcher.getConfig().get(LauncherProperties.SERVER_LAUNCH_FOR_EULA, true)) {
                log.info("Launching server to create EULA...");
                Process process = launch0(true);
                try {
                    if (process != null) {
                        process.waitFor();
                    }
                } catch (InterruptedException e) {
                    throw new CommandException(e);
                }
            }

            if (!server.hasEula(isInMemory())) {
                throw new CommandException("EULA could not be found for server " + server.getName());
            }
        }
    }

    public @Nullable Process launch() throws CommandException, LaunchException, IOException {
        if (launcher.getConfig().get(LauncherProperties.SERVER_ACCEPT_EULA, false)) {
            eulaLaunch();
            try {
                log.info("Accepting EULA...");
                acceptEula();
            } catch (IOException e) {
                throw new LaunchException("EULA could not be accepted for server " + server.getName(), e);
            }
        }

        return launch0(false);
    }

    private @Nullable Process launch0(boolean eula) throws CommandException, LaunchException, IOException {
        boolean serverTest = launcher.getConfig().get(LauncherProperties.SERVER_TEST, false);
        Path serverExecutable = server.getExecutable(launcher.getProcessFactory().getOs());
        boolean isJar = serverExecutable.toString().endsWith(".jar");
        if (!Files.exists(serverExecutable)) {
            try {
                launcher.getServerManager().remove(server);
            } catch (IOException e) {
                log.error("Failed to delete server without server jar at " + server.getPath(), e);
            }

            throw new CommandException("Server " + server.getName() + " has no valid server jar!");
        }

        Version version = getVersion(server);
        Java java = getJava(version);
        LaunchOptions options = LaunchOptions.builder()
                .server(true)
                .account(new LaunchAccount("", "", "", "", ""))
                .version(version)
                .launcher(launcher)
                .files(new FileManager(server.getPath().toAbsolutePath().toString()))
                .closeCommandLine(!prepare)
                .parseFlags(launcher, quit, args)
                .prepare(prepare)
                .build();

        if (options.isPrepare()) {
            return null; // TODO: this should accept EULA maybe?
        }

        if (!eula && (options.isCloseCommandLine() || options.isInMemory())) {
            launcher.getCommandLine().close();
        }

        if (options.isInMemory()) {
            if (!isJar) {
                throw new LaunchException("Server " + server.getName()
                        + " is currently not supported for in-memory launching as it does not use a Jar.");
            }

            if (serverTest) {
                throw new LaunchException("Server Test not supported for in-memory launching");
            }

            System.setProperty("log4j.shutdownHookEnabled", "false");

            try {
                System.gc(); // suggest JVM to clean up before running mc
                new ServerInMemoryLauncher(options, launcher.getJavaService().getCurrent(), server).launch();
            } catch (AuthException e) {
                throw new IllegalStateException(e);
            }

            joinThread("ServerMain");
            joinThread("Server thread");
            return null;
        }

        List<String> command = new ArrayList<>();
        if (isJar) {
            command.add(java.getExecutable());
            command.addAll(options.getAdditionalJvmArgs());
            command.add("-jar");
            command.add(serverExecutable.toAbsolutePath().toString());
        } else {
            if (serverExecutable.toString().endsWith(".bat")) {
                serverExecutable = createNonPausingBatFile(serverExecutable);
                command.add("cmd.exe");
                command.add("/c");
            } else if (!serverExecutable.toString().endsWith(".sh")) {
                throw new LaunchException("Cannot start server executable " + serverExecutable);
            }

            command.add(serverExecutable.toAbsolutePath().toString());
        }

        // TODO: specify server args in launch command
        String[] defaultServerArgs = launcher.getConfig().get(
                LauncherProperties.SERVER_ARGS,
                new String[] { "nogui" }
        );

        command.addAll(Arrays.asList(defaultServerArgs));
        command.addAll(options.getAdditionalGameArgs());

        log.debug("Launching server " + command);
        System.gc(); // suggest JVM to clean up before running mc
        ProcessBuilder processBuilder = new ProcessBuilder()
                .command(command)
                .directory(server.getPath().toFile())
                .redirectError(options.isNoOut()
                        ? ProcessBuilder.Redirect.PIPE
                        : ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(options.isNoOut()
                        ? ProcessBuilder.Redirect.PIPE
                        : ProcessBuilder.Redirect.INHERIT)
                .redirectInput(options.isNoIn()
                        ? ProcessBuilder.Redirect.PIPE
                        : ProcessBuilder.Redirect.INHERIT);
        if (!isJar) {
            Map<String, String> environment = processBuilder.environment();
            String path = environment.get("PATH");
            if (path != null) {
                environment.put("PATH",
                        Paths.get(java.getPath()).resolve("bin") + File.pathSeparator + path);
            } else {
                environment.put("PATH",
                        Paths.get(java.getPath()).resolve("bin").toString());
            }
        }

        return processBuilder.start();
    }

    // Forge .bat ends with pause so the program always ends with "Press any key to continue..."
    private Path createNonPausingBatFile(Path bat) throws IOException {
        List<String> lines = Files.readAllLines(bat);
        List<String> filteredLines = lines.stream()
                .filter(line -> !line.trim().toLowerCase(Locale.ENGLISH).contains("pause"))
                .collect(Collectors.toList());

        Path result = bat.getParent().resolve("hmc_run_server.bat");
        Files.write(result, filteredLines);
        return result;
    }

    private void joinThread(String name) throws LaunchException {
        try {
            Thread.sleep(1_000); // ensure Thread has started.
            // The ServerBundler and Server start the Server on another Thread
            // TODO: is it better to use getAllStackTraces or iterate ThreadGroups?
            Thread thread = Thread.getAllStackTraces()
                    .keySet()
                    .stream()
                    .filter(t -> name.equals(t.getName()))
                    .findFirst()
                    .orElse(null);

            if (thread != null) {
                log.info("Joining " + name + " Thread.");
                thread.join();
                log.info(name + " Thread ended.");
            }
        } catch (InterruptedException e) {
            throw new LaunchException(e);
        }
    }

    private Version getVersion(Server server) throws CommandException {
        VersionInfo versionInfo = launcher.getVersionInfoCache().getByName(server.getVersion().getVersion());
        if (versionInfo == null) {
            throw new CommandException("Failed to find version '" + server.getVersion().getVersion() + "'!");
        }

        Version version;
        try {
            version = VersionInfoUtil.toVersion(versionInfo, launcher.getVersionService(), launcher.getDownloadService());
        } catch (IOException e) {
            throw new CommandException("Failed to read Version " + versionInfo.getName(), e);
        }

        return version;
    }

    private Java getJava(Version version) throws CommandException {
        Java java = launcher.getJavaService().findBestVersion(launcher, version.getJava());
        if (java == null) {
            throw new CommandException("Failed to find Java version for "
                    + version.getName() + ", required: " + version.getJava());
        }

        return java;
    }

    private boolean isInMemory() {
        return CommandUtil.hasFlag("-inmemory", args)
                || launcher.getConfig().get(ALWAYS_IN_MEMORY, false);
    }

}

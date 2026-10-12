package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.os.OS;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Launches server start scripts, like the {@code run.sh}/{@code run.bat} of newer Forge and NeoForge servers.
 * By default, the script is launched directly with {@link #launcher(Profile, Path, Java, String, boolean)}.
 * If {@link ScriptConfig#mockJava()} is enabled, the script is instead run with a mock {@code java} command
 * first on the {@code PATH}, see {@link #captureJavaCommand(Path, Path)}.
 * The mock writes the arguments it received to a file, so we can parse them into a {@link JavaCommand}
 * and launch the server with a {@link io.github.headlesshq.headlessmc.java.launcher.JavaProcessBuilder}.
 *
 * @see ScriptConfig
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ScriptLauncher {
    static final String ARGS_FILE_ENV = "HMC_JAVA_ARGS_FILE";
    static final String NON_PAUSING_BAT = "hmc_run_server.bat";

    // Every call overwrites the args file, so the last java call of a script is captured.
    // Forge scripts first call java with --onlyCheckJava and then launch the server.
    private static final String UNIX_MOCK_JAVA = """
        #!/bin/sh
        for arg in "$@"; do printf '%s\\0' "$arg"; done > "$HMC_JAVA_ARGS_FILE"
        """;

    // %* is the raw command line, it gets split by splitWindowsCommandLine
    private static final String WINDOWS_MOCK_JAVA = """
        @echo off\r
        >"%HMC_JAVA_ARGS_FILE%" echo(%*\r
        exit /b 0\r
        """;

    private final Holder<ScriptConfig> config;
    private final OS os;

    /**
     * @return {@code true} if scripts should be launched by capturing their java command.
     */
    public boolean isMockJava() {
        return config.get().mockJava();
    }

    /**
     * Runs the given script with a mock {@code java} command on the {@code PATH}
     * and captures the arguments the script passes to it.
     *
     * @param script     the script to run.
     * @param workingDir the directory to run the script in.
     * @return the java command the script tried to run.
     * @throws ServerException if the script failed to call java.
     */
    public JavaCommand captureJavaCommand(Path script, Path workingDir) throws ServerException {
        Path mockDir = null;
        try {
            mockDir = Files.createTempDirectory("hmc-mock-java");
            Path argsFile = mockDir.resolve("args");
            Path output = mockDir.resolve("output.log");
            writeMockJava(mockDir);

            ProcessBuilder processBuilder = new ProcessBuilder(scriptCommand(script))
                .directory(workingDir.toAbsolutePath().toFile())
                .redirectErrorStream(true)
                .redirectOutput(output.toFile());
            prependToPath(processBuilder.environment(), mockDir);
            processBuilder.environment().put(ARGS_FILE_ENV, argsFile.toAbsolutePath().toString());

            log.debug("Capturing java command of {} with {}", script, processBuilder.command());
            Process process = processBuilder.start();
            // closing STD_IN makes a pause in a .bat file return immediately
            process.getOutputStream().close();
            if (!process.waitFor(config.get().timeout(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new ServerException("Script " + script + " did not finish within "
                    + config.get().timeout() + " seconds: " + readOutput(output));
            }

            if (!Files.exists(argsFile)) {
                throw new ServerException("Script " + script + " did not call java, exit code "
                    + process.exitValue() + ": " + readOutput(output));
            }

            List<String> arguments = readArguments(argsFile);
            log.debug("Script {} called java with {}", script, arguments);
            return JavaCommand.parse(arguments, workingDir.toAbsolutePath(), argFileCharset());
        } catch (IOException e) {
            throw new ServerException("Failed to capture java command of " + script, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServerException("Interrupted while capturing java command of " + script, e);
        } finally {
            deleteQuietly(mockDir);
        }
    }

    /**
     * Creates a launcher that runs the script directly,
     * with the bin directory of the given java first on the {@code PATH}.
     * The vm args and system properties of the profile are passed via {@code JDK_JAVA_OPTIONS}.
     *
     * @param profile     the profile of the server.
     * @param script      the script to run.
     * @param java        the java to use for the script.
     * @param processName the name of the process.
     * @param pipe        whether to pipe IO.
     * @return a launcher for the script.
     */
    public ProcessLauncher launcher(Profile profile, Path script, Java java, String processName, boolean pipe) {
        List<String> command = new ArrayList<>(scriptCommand(script));
        if (profile.gameArgs() != null) {
            command.addAll(profile.gameArgs());
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command).directory(profile.path().toAbsolutePath().toFile());
        if (pipe) {
            processBuilder
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectInput(ProcessBuilder.Redirect.PIPE);
        } else {
            processBuilder.inheritIO();
        }

        Path javaExecutable = java.executable()
            .get(FileSystems.getDefault())
            .orElseThrow(() -> new ServerException(java + " is not available on the default filesystem"));
        prependToPath(processBuilder.environment(), javaExecutable.toAbsolutePath().getParent());

        List<String> jvmOptions = new ArrayList<>(profile.vmArgs());
        profile.systemProperties().forEach((key, value) -> jvmOptions.add("-D" + key + (value == null ? "" : "=" + value)));
        if (!jvmOptions.isEmpty()) {
            processBuilder.environment().put("JDK_JAVA_OPTIONS", jvmOptions.stream()
                .map(ScriptLauncher::quoteJavaOption)
                .reduce((a, b) -> a + " " + b)
                .orElseThrow());
        }

        log.debug("Launching script {} directly", processBuilder.command());
        return ProcessLauncher.ofScript(processName, processBuilder, profile.path());
    }

    @VisibleForTesting
    List<String> scriptCommand(Path script) {
        String name = script.getFileName().toString().toLowerCase(Locale.ENGLISH);
        if (name.endsWith(".bat") || name.endsWith(".cmd")) {
            return List.of("cmd.exe", "/c", createNonPausingBatFile(script).toAbsolutePath().toString());
        }

        if (isWindows()) {
            throw new ServerException("Cannot run script " + script + " on Windows");
        }

        return List.of("sh", script.toAbsolutePath().toString());
    }

    // Forge .bat files end with pause, so the program always ends with "Press any key to continue..."
    // Calling the mock java.bat without "call" would never return to the script,
    // and Forge scripts call java twice, first with --onlyCheckJava, so we call java.
    private Path createNonPausingBatFile(Path bat) {
        try {
            List<String> lines = Files.readAllLines(bat, Charset.defaultCharset()).stream()
                .filter(line -> !line.trim().toLowerCase(Locale.ENGLISH).equals("pause"))
                .map(ScriptLauncher::callJava)
                .toList();
            Path result = bat.toAbsolutePath().resolveSibling(NON_PAUSING_BAT);
            Files.write(result, lines, Charset.defaultCharset());
            return result;
        } catch (IOException e) {
            throw new ServerException("Failed to create non-pausing copy of " + bat, e);
        }
    }

    private static String callJava(String line) {
        String trimmed = line.stripLeading();
        String lowerCase = trimmed.toLowerCase(Locale.ENGLISH);
        if (lowerCase.startsWith("java ") || lowerCase.startsWith("java.exe ")) {
            return line.substring(0, line.length() - trimmed.length()) + "call " + trimmed;
        }

        return line;
    }

    private void writeMockJava(Path mockDir) throws IOException {
        if (isWindows()) {
            Files.writeString(mockDir.resolve("java.bat"), WINDOWS_MOCK_JAVA, StandardCharsets.US_ASCII);
        } else {
            Path java = mockDir.resolve("java");
            Files.writeString(java, UNIX_MOCK_JAVA, StandardCharsets.UTF_8);
            Files.setPosixFilePermissions(java, PosixFilePermissions.fromString("rwxr-xr-x"));
        }
    }

    private List<String> readArguments(Path argsFile) throws IOException {
        if (isWindows()) {
            return splitWindowsCommandLine(Files.readString(argsFile, Charset.defaultCharset()).strip());
        }

        String content = Files.readString(argsFile, StandardCharsets.UTF_8);
        if (content.isEmpty()) {
            return List.of();
        }

        // every argument is terminated by \0
        return Arrays.asList(content.substring(0, content.length() - 1).split("\0", -1));
    }

    private Charset argFileCharset() {
        // the java launcher reads argument files in the native encoding
        String encoding = System.getProperty("native.encoding");
        try {
            return encoding == null ? Charset.defaultCharset() : Charset.forName(encoding);
        } catch (IllegalArgumentException e) {
            return Charset.defaultCharset();
        }
    }

    private boolean isWindows() {
        return OS.Type.WINDOWS.equals(os.type());
    }

    /**
     * Splits a Windows command line, as given by {@code %*} in a .bat file, into arguments.
     * Arguments are separated by whitespace and can be quoted with {@code "}.
     *
     * @param commandLine the command line to split.
     * @return the arguments of the command line.
     */
    static List<String> splitWindowsCommandLine(String commandLine) {
        List<String> result = new ArrayList<>();
        StringBuilder current = null;
        boolean quoted = false;
        for (int i = 0; i < commandLine.length(); i++) {
            char c = commandLine.charAt(i);
            if (c == '"') {
                quoted = !quoted;
                if (current == null) {
                    current = new StringBuilder();
                }
            } else if (!quoted && Character.isWhitespace(c)) {
                if (current != null) {
                    result.add(current.toString());
                    current = null;
                }
            } else {
                if (current == null) {
                    current = new StringBuilder();
                }

                current.append(c);
            }
        }

        if (current != null) {
            result.add(current.toString());
        }

        return result;
    }

    private static String quoteJavaOption(String option) {
        if (option.chars().noneMatch(c -> Character.isWhitespace(c) || c == '"' || c == '\'')) {
            return option;
        }

        return "\"" + option.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static void prependToPath(Map<String, String> environment, Path dir) {
        // on Windows the key might not be all upper case
        String key = environment.keySet().stream()
            .filter(name -> name.equalsIgnoreCase("PATH"))
            .findFirst()
            .orElse("PATH");
        String path = environment.get(key);
        environment.put(key, path == null || path.isEmpty()
            ? dir.toAbsolutePath().toString()
            : dir.toAbsolutePath() + File.pathSeparator + path);
    }

    private static String readOutput(Path output) {
        try {
            return Files.exists(output) ? Files.readString(output, Charset.defaultCharset()).strip() : "";
        } catch (IOException e) {
            return "<failed to read output: " + e.getMessage() + ">";
        }
    }

    private static void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }

        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.toList()) {
                Files.deleteIfExists(file);
            }

            Files.deleteIfExists(dir);
        } catch (IOException e) {
            log.warn("Failed to delete {}", dir, e);
        }
    }

}

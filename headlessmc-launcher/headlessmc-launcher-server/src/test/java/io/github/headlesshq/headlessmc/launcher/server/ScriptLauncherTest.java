package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.os.OS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScriptLauncherTest {
    @TempDir
    Path root;

    @Test
    void windowsCommandLinesAreSplit() {
        assertEquals(
            List.of("@user_jvm_args.txt", "@libraries/win_args.txt", "with space", "nogui"),
            ScriptLauncher.splitWindowsCommandLine("@user_jvm_args.txt  @libraries/win_args.txt \"with space\" nogui")
        );
        assertEquals(List.of(), ScriptLauncher.splitWindowsCommandLine(""));
    }

    @Test
    void batFilesAreCopiedWithoutPauseAndWithJavaCalls() throws IOException {
        Path bat = root.resolve("run.bat");
        Files.write(bat, List.of(
            "@echo off",
            "java -jar forge-shim.jar --onlyCheckJava",
            "if %ERRORLEVEL% NEQ 0 (",
            "    goto :exit",
            ")",
            "java @user_jvm_args.txt @libraries/win_args.txt %*",
            ":exit",
            "pause"
        ));

        ScriptLauncher launcher = new ScriptLauncher(() -> null, new OS("windows", OS.Type.WINDOWS, "11"));
        Path copy = root.resolve(ScriptLauncher.NON_PAUSING_BAT);

        assertEquals(List.of("cmd.exe", "/c", copy.toAbsolutePath().toString()), launcher.scriptCommand(bat));
        assertEquals(List.of(
            "@echo off",
            "call java -jar forge-shim.jar --onlyCheckJava",
            "if %ERRORLEVEL% NEQ 0 (",
            "    goto :exit",
            ")",
            "call java @user_jvm_args.txt @libraries/win_args.txt %*",
            ":exit"
        ), Files.readAllLines(copy));
    }

}

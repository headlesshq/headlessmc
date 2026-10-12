package io.github.headlesshq.headlessmc.launcher.server;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScriptLauncherTest {
    @Test
    void windowsCommandLinesAreSplit() {
        assertEquals(
            List.of("@user_jvm_args.txt", "@libraries/win_args.txt", "with space", "nogui"),
            ScriptLauncher.splitWindowsCommandLine("@user_jvm_args.txt  @libraries/win_args.txt \"with space\" nogui")
        );
        assertEquals(List.of(), ScriptLauncher.splitWindowsCommandLine(""));
    }

}

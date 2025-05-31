package io.github.headlesshq.headlessmc.launcher.launch;

import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.launcher.files.ConfigService;
import io.github.headlesshq.headlessmc.launcher.util.IOUtil;
import io.github.headlesshq.headlessmc.os.OS;
import lombok.Cleanup;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Locale;

@CustomLog
@RequiredArgsConstructor
public class XvfbService {
    private final ConfigService configService;
    private final OS os;

    private boolean checked;
    private boolean runningWithXvfb;

    public boolean isRunningWithXvfb() {
        if (!checked) {
            try {
                runningWithXvfb = checkRunningWithXvfb();
            } catch (IOException e) {
                log.error(e);
            } finally {
                checked = true;
            }
        }

        return runningWithXvfb;
    }

    private boolean checkRunningWithXvfb() throws IOException {
        if (os.getType() != OS.Type.LINUX || !configService.getConfig().get(LauncherProperties.CHECK_XVFB, false)) {
            return false;
        }

        Process process = new ProcessBuilder().command("ps", "aux").start();
        @Cleanup
        BufferedReader reader = IOUtil.reader(process.getInputStream());
        String output = IOUtil.read(reader);
        boolean result = output.toLowerCase(Locale.ENGLISH).contains("xvfb");
        log.info(result ? "Running with xvfb" : "Not running with xvfb");
        return result;
    }

}

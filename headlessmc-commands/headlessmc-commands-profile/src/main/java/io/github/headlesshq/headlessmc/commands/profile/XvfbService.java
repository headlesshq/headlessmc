package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.os.OSService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class XvfbService {
    private final Holder<XvfbConfig> config;
    private final OSService osService;

    private boolean checked;
    private boolean runningWithXvfb;

    public boolean isRunningWithXvfb() {
        if (!checked) {
            try {
                runningWithXvfb = checkRunningWithXvfb();
            } catch (IOException e) {
                log.error("Failed to check if running with XVFB", e);
            } finally {
                checked = true;
            }
        }

        return runningWithXvfb;
    }

    private boolean checkRunningWithXvfb() throws IOException {
        OS os = osService.getOS();
        if (!OS.McType.LINUX.equals(os.type().mcType()) || !config.get().check()) {
            return false;
        }

        // use ProcessHandle.allProcesses()?
        Process process = new ProcessBuilder().command("ps", "aux").start();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String output = bufferedReader.readAllAsString();
            boolean result = output.toLowerCase(Locale.ENGLISH).contains("xvfb");
            log.info(result ? "Running with xvfb" : "Not running with xvfb");
            return result;
        }
    }

}

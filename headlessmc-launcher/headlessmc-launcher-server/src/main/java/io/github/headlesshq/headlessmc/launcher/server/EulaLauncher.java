package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.java.launcher.ProcessHandler;
import io.github.headlesshq.headlessmc.launcher.process.McProcess;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.EulaStatus;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@RequiredArgsConstructor
public class EulaLauncher {
    private final ServerLauncher serverLauncher;

    public Optional<Eula> eulaLaunch(Profile profile, ServerService serverService) {
        if (EulaStatus.NONE.equals(profile.eulaStatus())) {
            return Optional.empty();
        }

        Eula eula = new Eula(profile.path().resolve("eula.txt"));
        if (eula.exists()) {
            serverService.save(profile.withEulaStatus(EulaStatus.EXISTS));
            return Optional.of(eula);
        }

        ProcessLauncher launcher = serverLauncher.launchProcess(profile, "mc-eula-" + profile.name(), true);
        McProcess serverProcess = launcher.launch(true);
        // Stop server immediately after starting
        // --initSettings is also a thing in newer versions, but not in older versions
        // TODO: inMemory launcher needs to replace System.in and configure servers Jline to read from it, so this works
        serverProcess.process().ifPresent(process -> {
            try (OutputStream outputStream = process.getOutputStream()) {
                if (outputStream == null) {
                    throw new ServerException("Failed to get server STD_IN");
                }

                outputStream.write(("stop" + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (IOException e) {
                throw new ServerException("Failed to get server STD_IN", e);
            }
        });

        int result = serverProcess.waitFor(ProcessHandler.defaultHandler());
        if (eula.exists()) {
            serverService.save(profile.withEulaStatus(EulaStatus.EXISTS));
            return Optional.of(eula);
        }

        if (result != 0) {
            throw new ServerException("EULA launch for " + profile.name() + " failed with exit code " + result);
        }

        serverService.save(profile.withEulaStatus(EulaStatus.NONE));
        return Optional.empty();
    }

}

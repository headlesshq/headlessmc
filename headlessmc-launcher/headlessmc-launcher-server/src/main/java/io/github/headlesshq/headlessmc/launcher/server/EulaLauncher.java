package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.java.launcher.JavaProcess;
import io.github.headlesshq.headlessmc.java.launcher.ProcessHandler;
import io.github.headlesshq.headlessmc.launcher.process.McProcess;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.EulaStatus;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class EulaLauncher {
    private final ServerLauncher serverLauncher;

    /**
     * Finds the eula.txt of a server. A server that has none yet is launched once to create it.
     *
     * <p>Nothing is saved by this method. The given profile is the profile a server is launched with,
     * it can carry options of a single launch, like additional jvm arguments, which are used to launch the server
     * here as well, but must never end up in a stored profile. Storing the {@link Result#status() status} is up
     * to the caller, who knows the profile as it is stored.</p>
     *
     * @param profile the server to find the eula.txt of.
     * @return the eula.txt, if the server has one, and the status to remember for the server.
     * @throws ServerException if the server has to be launched and fails without creating a eula.txt.
     */
    public Result eulaLaunch(Profile profile) {
        if (EulaStatus.NONE.equals(profile.eulaStatus())) {
            return new Result(EulaStatus.NONE, Optional.empty());
        }

        Eula eula = new Eula(profile.path().resolve("eula.txt"));
        if (eula.exists()) {
            return new Result(EulaStatus.EXISTS, Optional.of(eula));
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

            drainOutput(process);
        });

        int result = serverProcess.waitFor(ignoringExitCode(ProcessHandler.defaultHandler()));
        if (eula.exists()) {
            return new Result(EulaStatus.EXISTS, Optional.of(eula));
        }

        if (result != 0) {
            throw new ServerException("EULA launch for " + profile.name() + " failed with exit code " + result);
        }

        return new Result(EulaStatus.NONE, Optional.empty());
    }

    /**
     * What {@link #eulaLaunch(Profile)} found out.
     *
     * @param status the {@link Profile#eulaStatus()} to remember for the server, so that a server without a
     *               eula.txt is not launched again to look for one.
     * @param eula   the eula.txt of the server, if it has one.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public record Result(EulaStatus status, Optional<Eula> eula) {

    }

    private static ProcessHandler ignoringExitCode(ProcessHandler handler) {
        return new ProcessHandler() {
            @Override
            public void handle(String process, int exitCode) {
                // ignored
            }

            @Override
            public void interrupted(JavaProcess process) {
                handler.interrupted(process);
            }

            @Override
            public void interrupted(String id, Process process) {
                handler.interrupted(id, process);
            }
        };
    }

    /**
     * Reads the output of the process until it exits.
     * The output is piped, if nobody reads it the process blocks
     * as soon as the pipe buffer (only ~4KB on Windows) is full.
     */
    private void drainOutput(Process process) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("[EULA] {}", line);
            }
        } catch (IOException e) {
            log.warn("Failed to read output of EULA launch", e);
        }
    }

}

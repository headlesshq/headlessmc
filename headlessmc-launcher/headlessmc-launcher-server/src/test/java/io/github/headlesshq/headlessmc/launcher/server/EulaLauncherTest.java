package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessBuilder;
import io.github.headlesshq.headlessmc.launcher.process.McProcess;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.EulaStatus;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EulaLauncherTest {
    private static final String EULA = "#EULA\neula=false\n";

    @TempDir
    Path root;

    private final java.util.List<String> launches = new java.util.ArrayList<>();

    private boolean writeEulaOnLaunch;

    private FakeProfileService profileService;
    private ServerServiceImpl serverService;
    private EulaLauncher eulaLauncher;

    @BeforeEach
    void setup() {
        profileService = new FakeProfileService(root);
        serverService = new ServerServiceImpl(profileService);
        eulaLauncher = new EulaLauncher(recordingLauncher(Optional.empty()));
    }

    /**
     * A {@link ServerLauncher} that records the launch and, if
     * {@link #writeEulaOnLaunch} is set, creates the eula.txt a real server would.
     *
     * @param process the process the launch reports back, e.g. to capture its stdin.
     */
    private ServerLauncher recordingLauncher(Optional<Process> process) {
        ServerLauncher launcher = Mockito.mock(ServerLauncher.class);
        Mockito.when(launcher.launchProcess(Mockito.any(), Mockito.any(), Mockito.anyBoolean()))
            .thenAnswer(invocation -> {
                Profile profile = invocation.getArgument(0);
                String processName = invocation.getArgument(1);
                launches.add(processName);
                if (writeEulaOnLaunch) {
                    write(profile, EULA);
                }

                return new ProcessLauncher() {
                    @Override
                    public McProcess launch(boolean pipeIO) throws HeadlessMcException {
                        return new McProcess(processName, Optional.empty(), process);
                    }

                    @Override
                    public Optional<JavaProcessBuilder> getJavaProcessBuilder() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<ProcessBuilder> getProcessBuilder() {
                        return Optional.empty();
                    }

                    @Override
                    public Path getGameDir() {
                        return profile.path();
                    }

                    @Override
                    public String getId() {
                        return processName;
                    }
                };
            });

        return launcher;
    }

    private Profile server(String name) {
        Path dir = root.resolve(name);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return profileService.save(new Profile(name, VersionArg.parse("server", "vanilla", "1.21.1"), dir));
    }

    private void write(Profile profile, String content) {
        try {
            Files.writeString(profile.path().resolve("eula.txt"), content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void serversWithoutAEulaAreSkipped() {
        Profile profile = server("srv").withEulaStatus(EulaStatus.NONE);

        assertEquals(Optional.empty(), eulaLauncher.eulaLaunch(profile, serverService));
        assertEquals(java.util.List.of(), launches);
    }

    @Test
    void existingEulaIsReturnedWithoutLaunching() {
        Profile profile = server("srv");
        write(profile, EULA);

        Optional<Eula> eula = eulaLauncher.eulaLaunch(profile, serverService);

        assertTrue(eula.isPresent());
        assertEquals(java.util.List.of(), launches);
        assertEquals(EulaStatus.EXISTS, profileService.getProfile("srv").orElseThrow().eulaStatus());
    }

    @Test
    void serverIsLaunchedOnceToGenerateTheEula() {
        writeEulaOnLaunch = true;
        Profile profile = server("srv");

        Optional<Eula> eula = eulaLauncher.eulaLaunch(profile, serverService);

        assertTrue(eula.isPresent());
        assertEquals(java.util.List.of("mc-eula-srv"), launches);
        assertEquals(EulaStatus.EXISTS, profileService.getProfile("srv").orElseThrow().eulaStatus());
    }

    @Test
    void serversThatNeverCreateAEulaAreMarkedAsHavingNone() {
        Profile profile = server("srv");

        assertEquals(Optional.empty(), eulaLauncher.eulaLaunch(profile, serverService));
        assertEquals(EulaStatus.NONE, profileService.getProfile("srv").orElseThrow().eulaStatus());
    }

    @Test
    void theStopCommandIsWrittenToTheServerStdIn() {
        ByteArrayOutputStream stdIn = new ByteArrayOutputStream();
        writeEulaOnLaunch = true;
        EulaLauncher launcher = new EulaLauncher(recordingLauncher(Optional.of(new StdInProcess(stdIn))));

        Profile profile = server("srv");
        assertTrue(launcher.eulaLaunch(profile, serverService).isPresent());
        assertEquals("stop" + System.lineSeparator(), stdIn.toString(StandardCharsets.UTF_8));
    }

    /** Minimal {@link Process} that only exposes an output stream. */
    private static final class StdInProcess extends Process {
        private final OutputStream outputStream;

        StdInProcess(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public OutputStream getOutputStream() {
            return outputStream;
        }

        @Override
        public java.io.InputStream getInputStream() {
            return java.io.InputStream.nullInputStream();
        }

        @Override
        public java.io.InputStream getErrorStream() {
            return java.io.InputStream.nullInputStream();
        }

        @Override
        public int waitFor() {
            return 0;
        }

        @Override
        public int exitValue() {
            return 0;
        }

        @Override
        public void destroy() {
        }
    }

}

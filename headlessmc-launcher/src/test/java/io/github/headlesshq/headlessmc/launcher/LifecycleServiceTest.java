package io.github.headlesshq.headlessmc.launcher;

import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.java.launcher.FakeJavaProcessManager;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileLaunchEvent;
import io.github.headlesshq.headlessmc.test.CommandTestService;
import io.github.headlesshq.headlessmc.test.TestConfig;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import jakarta.enterprise.event.Event;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LifecycleServiceTest {
    @Test
    void wrapCreatesALifecycleForTheLauncher() {
        LauncherConfig config = Mockito.mock(LauncherConfig.class);
        TestConfig testConfig = Mockito.mock(TestConfig.class);
        RecordingConsole console = new RecordingConsole();
        CommandTestService testService = new CommandTestService(() -> testConfig, new DefaultJacksonJsonService());
        ProcessLauncher launcher = Mockito.mock(ProcessLauncher.class);
        Mockito.when(launcher.getGameDir()).thenReturn(Path.of("game"));

        FakeJavaProcessManager processManager = new FakeJavaProcessManager();
        LifecycleService service = new LifecycleService(
            testService, () -> config, () -> testConfig, console, processManager, event()
        );
        ProcessLifecycle lifecycle = service.wrap(launcher, 3);

        assertSame(launcher, lifecycle.getProcessFactory());
        assertSame(testService, lifecycle.getTestService());
        assertSame(config, lifecycle.getConfig());
        assertSame(testConfig, lifecycle.getTestConfig());
        assertSame(console, lifecycle.getConsole());
        assertEquals(Path.of("game"), lifecycle.getGameDir());
        assertEquals(3, lifecycle.getRetries());
        assertSame(processManager, lifecycle.getProcessManager());
    }

    @Test
    void beforeLaunchFiresLaunchEvent() {
        Event<ProfileLaunchEvent> event = event();
        LifecycleService service = new LifecycleService(
            Mockito.mock(CommandTestService.class), Mockito::mock, Mockito::mock,
            new RecordingConsole(), new FakeJavaProcessManager(), event
        );
        Profile profile = new Profile("main", VersionArg.parse("1.21.1"), Path.of("game"));

        service.beforeLaunch(profile, false);

        Mockito.verify(event).fire(new ProfileLaunchEvent(profile, false));
    }

    @SuppressWarnings("unchecked")
    private Event<ProfileLaunchEvent> event() {
        return Mockito.mock(Event.class);
    }

}

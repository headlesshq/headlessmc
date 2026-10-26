package io.github.headlesshq.headlessmc.updater;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.LongRunningCommand;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UpdateNotifierTest {
    private final Console console = Mockito.mock(Console.class);
    private final UpdaterConfig config = Mockito.mock(UpdaterConfig.class);
    private final Updater java = updater("java", true);
    private final Updater headlessmc = updater("headlessmc", true);
    private final Updater disabled = updater("disabled", false);
    private UpdateNotifier notifier;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setup() {
        when(config.enabled()).thenReturn(true);
        Instance<Updater> updaters = Mockito.mock(Instance.class);
        when(updaters.stream()).thenAnswer(invocation -> Stream.of(java, disabled, headlessmc));
        Holder<UpdaterConfig> holder = () -> config;
        notifier = new UpdateNotifier(updaters, holder, console);
    }

    @Test
    public void testGetUpdaters() {
        assertEquals(List.of(headlessmc, java), notifier.getUpdaters());
        when(config.enabled()).thenReturn(false);
        assertEquals(List.of(), notifier.getUpdaters());
    }

    @Test
    public void testCheckBeforeLongRunningCommand() {
        when(headlessmc.checkForUpdatesIfStale()).thenReturn(Optional.of("update headlessmc"));
        when(java.checkForUpdatesIfStale()).thenThrow(new IllegalStateException("failed"));

        notifier.checkBefore(new LongRunningCommand() {});

        verify(console).write("update headlessmc");
        verify(java).checkForUpdatesIfStale();
        verify(disabled, never()).checkForUpdatesIfStale();
        verify(headlessmc, never()).checkForUpdates();
    }

    @Test
    public void testCheckBeforeOtherCommand() {
        notifier.checkBefore(new Object());
        notifier.checkBefore(null);

        verifyNoInteractions(console);
        verify(headlessmc, never()).checkForUpdatesIfStale();
    }

    @Test
    public void testCheckAsync() {
        when(java.checkForUpdates()).thenReturn(Optional.of("update java"));

        notifier.checkAsync();

        verify(console, timeout(5_000)).write("update java");
        verify(headlessmc, timeout(5_000)).checkForUpdates();
        verify(disabled, never()).checkForUpdates();
    }

    private static Updater updater(String name, boolean enabled) {
        Updater updater = Mockito.mock(Updater.class);
        when(updater.getName()).thenReturn(name);
        when(updater.isEnabled()).thenReturn(enabled);
        when(updater.checkForUpdates()).thenReturn(Optional.empty());
        when(updater.checkForUpdatesIfStale()).thenReturn(Optional.empty());
        return updater;
    }

}

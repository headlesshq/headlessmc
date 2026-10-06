package io.github.headlesshq.headlessmc.test;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * A {@link TimeoutHandler} that records the timeouts set on it instead of scheduling them.
 */
public class RecordingTimeoutHandler extends TimeoutHandler {
    public final List<Long> timeouts = new ArrayList<>();
    public @Nullable Long current;

    public RecordingTimeoutHandler() {
        super(Executors.newSingleThreadScheduledExecutor(), () -> {});
        close();
    }

    @Override
    public void setTimeout(long seconds) {
        timeouts.add(seconds);
        current = seconds;
    }

    @Override
    public void removeTimeout() {
        current = null;
    }

    @Override
    public boolean hasTimeout() {
        return current != null;
    }

}

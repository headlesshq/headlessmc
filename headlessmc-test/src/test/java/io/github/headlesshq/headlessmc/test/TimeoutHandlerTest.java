package io.github.headlesshq.headlessmc.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class TimeoutHandlerTest {
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private final CountDownLatch timedOut = new CountDownLatch(1);
    private final TimeoutHandler timeoutHandler;

    public TimeoutHandlerTest() {
        executor.setRemoveOnCancelPolicy(true);
        timeoutHandler = new TimeoutHandler(executor, timedOut::countDown);
    }

    @AfterEach
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void hasNoTimeoutInitially() {
        assertFalse(timeoutHandler.hasTimeout());
    }

    @Test
    public void runsCallbackWhenTimeoutExpires() throws InterruptedException {
        timeoutHandler.setTimeout(0L);
        assertTrue(timeoutHandler.hasTimeout());
        assertTrue(timedOut.await(5, TimeUnit.SECONDS), "timeout callback was not called");
    }

    @Test
    public void removeTimeoutCancelsTheScheduledCallback() {
        timeoutHandler.setTimeout(60L);
        assertEquals(1, executor.getQueue().size());

        timeoutHandler.removeTimeout();
        assertFalse(timeoutHandler.hasTimeout());
        assertTrue(executor.getQueue().isEmpty());
        assertEquals(1, timedOut.getCount());
    }

    @Test
    public void removeTimeoutWithoutTimeoutDoesNothing() {
        timeoutHandler.removeTimeout();
        assertFalse(timeoutHandler.hasTimeout());
    }

    @Test
    public void setTimeoutReplacesThePreviousTimeout() {
        timeoutHandler.setTimeout(60L);
        timeoutHandler.setTimeout(120L);

        assertEquals(1, executor.getQueue().size());
        long delay = ((Delayed) executor.getQueue().iterator().next()).getDelay(TimeUnit.SECONDS);
        assertTrue(delay > 60L, "expected the second timeout to be scheduled, but delay was " + delay);
    }

    @Test
    public void closeCancelsTheTimeoutAndShutsDownTheExecutor() {
        timeoutHandler.setTimeout(60L);
        timeoutHandler.close();

        assertTrue(executor.isShutdown());
        assertTrue(executor.getQueue().isEmpty());
        assertEquals(1, timedOut.getCount());
    }

}

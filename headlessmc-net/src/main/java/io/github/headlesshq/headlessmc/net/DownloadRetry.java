package io.github.headlesshq.headlessmc.net;

import java.util.concurrent.Callable;

/**
 * A retry service takes care of retrying tasks.
 * One such task could be a Download that needs to be retried.
 *
 * @see DownloadBuilder#retry(DownloadRetry)
 */
@FunctionalInterface // apparently not, due to the generic task?
public interface DownloadRetry {
    /**
     * Calls the given {@link Callable} task.
     * Implementations should at least run the task once (first try).
     * If an Exception is thrown retry attempts may be made,
     * or the exception is propagated.
     *
     * @param task the task to run and maybe retry.
     * @return the result of the task.
     * @param <T> the type of the result.
     * @throws Exception thrown by the {@link Callable}.
     */
    <T> T run(Callable<T> task) throws Exception;

    /**
     * @return a {@link DownloadRetry} that performs exactly one attempt and no retries.
     */
    static DownloadRetry none() {
        return new DownloadRetry() {
            @Override
            public <T> T run(Callable<T> task) throws Exception {
                return task.call();
            }
        };
    }

}

package io.github.headlesshq.headlessmc.net.parallel;

import io.github.headlesshq.headlessmc.exceptions.ExRunnable;

/**
 * A unit of work, usually a single download, executed by a
 * {@link ParallelTaskService}.
 */
public interface ParallelTask extends ExRunnable {
    /**
     * @return the estimated size of this task in bytes, used to order tasks
     *         for scheduling and to report overall progress. May be {@code 0}
     *         if unknown.
     */
    long getSize();

}

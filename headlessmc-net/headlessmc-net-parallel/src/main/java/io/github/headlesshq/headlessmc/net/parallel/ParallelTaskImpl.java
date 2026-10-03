package io.github.headlesshq.headlessmc.net.parallel;

import io.github.headlesshq.headlessmc.exceptions.ExRunnable;
import org.jspecify.annotations.Nullable;

public record ParallelTaskImpl(@Nullable Long size, ExRunnable runnable) implements ParallelTask {
    @Override
    public long getSize() {
        return size == null ? 0L : size;
    }

    @Override
    public void run() throws Exception {
        runnable.run();
    }

}

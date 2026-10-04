package io.github.headlesshq.headlessmc.progressbar;

import org.jspecify.annotations.Nullable;

public interface ProgressBar extends AutoCloseable {
    boolean isDummy();

    void stepBy(long n);

    void stepTo(long n);

    void step();

    void maxHint(long n);

    @Override
    void close();

    static ProgressBar dummy() {
        return DummyProgressBarService.DUMMY;
    }

    record Configuration(String taskName, long initialMax, @Nullable Unit unit) {
        public static final long NO_INITIAL_SIZE = -1;

        public Configuration(String taskName, long initialMax) {
            this(taskName, initialMax, null);
        }

        public record Unit(String name, long size) {
            public static final Unit MB = new Unit("mb", 1_000_000);
        }
    }

}

package io.github.headlesshq.headlessmc.progressbar;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

/**
 * We abstract over {@code me.tongfei.progressbar.ProgressBar},
 * because it depends on JLine, which might not be available at runtime.
 * @see <a href="https://github.com/ctongfei/progressbar">https://github.com/ctongfei/progressbar</a>
 */
public interface Progressbar extends AutoCloseable {
    void stepBy(long n);

    void stepTo(long n);

    void step();

    void maxHint(long n);

    boolean isDummy();

    @Override
    void close();

    static Progressbar dummy() {
        return new Progressbar() {
            @Override
            public void close() {

            }

            @Override
            public void stepBy(long n) {

            }

            @Override
            public void stepTo(long n) {

            }

            @Override
            public void step() {

            }

            @Override
            public void maxHint(long n) {

            }

            @Override
            public boolean isDummy() {
                return true;
            }
        };
    }

    @Data
    @RequiredArgsConstructor
    class Configuration {
        private final String taskName;
        private final long initialMax;
        private final @Nullable Unit unit;

        public Configuration(String taskName, long initialMax) {
            this(taskName, initialMax, null);
        }

        @Data
        public static class Unit {
            private final String name;
            private final long max;
        }
    }

}

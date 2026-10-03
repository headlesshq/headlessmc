package io.github.headlesshq.headlessmc.progressbar;

import jakarta.enterprise.context.ApplicationScoped;

@Dummy
@ApplicationScoped
final class DummyProgressBarService implements ProgressbarService {
    public static final ProgressBar DUMMY = new ProgressBar() {
        @Override
        public boolean isDummy() {
            return true;
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
        public void close() {

        }
    };

    @Override
    public String getName() {
        return "dummy";
    }

    @Override
    public ProgressBar displayProgressBar(ProgressBar.Configuration configuration) {
        return DUMMY;
    }

    @Override
    public boolean isDummy() {
        return true;
    }

}

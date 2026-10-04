package io.github.headlesshq.headlessmc.progressbar.jline;

import lombok.RequiredArgsConstructor;
import me.tongfei.progressbar.ProgressBar;

/**
 * Implementation of {@link io.github.headlesshq.headlessmc.progressbar.ProgressBar}
 * that wraps a {@link me.tongfei.progressbar.ProgressBar}.
 */
@RequiredArgsConstructor
final class JlineProgressBar implements io.github.headlesshq.headlessmc.progressbar.ProgressBar {
    private final ProgressBar progressBar;

    @Override
    public boolean isDummy() {
        return false;
    }

    @Override
    public void stepBy(long n) {
        progressBar.stepBy(n);
    }

    @Override
    public void stepTo(long n) {
        progressBar.stepTo(n);
    }

    @Override
    public void step() {
        progressBar.step();
    }

    @Override
    public void maxHint(long n) {
        progressBar.maxHint(n);
    }

    @Override
    public void close() {
        progressBar.close();
    }

}

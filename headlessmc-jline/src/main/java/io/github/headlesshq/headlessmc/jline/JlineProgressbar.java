package io.github.headlesshq.headlessmc.jline;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import io.github.headlesshq.headlessmc.api.command.Progressbar;
import me.tongfei.progressbar.ProgressBar;

@Getter
@RequiredArgsConstructor
public class JlineProgressbar implements Progressbar {
    private final ProgressBar progressBar;

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

    @Override
    public boolean isDummy() {
        return false;
    }

}

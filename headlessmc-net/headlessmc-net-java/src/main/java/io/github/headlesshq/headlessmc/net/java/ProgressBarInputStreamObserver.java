package io.github.headlesshq.headlessmc.net.java;

import io.github.headlesshq.headlessmc.net.hash.ObservableInputStream;
import io.github.headlesshq.headlessmc.progressbar.ProgressBar;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class ProgressBarInputStreamObserver implements ObservableInputStream.Observer {
    private final ProgressBar progressBar;

    @Override
    public void byteRead(byte b) {
        progressBar.step();
    }

    @Override
    public void bytesRead(byte[] b, int off, int len) {
        progressBar.stepBy(len);
    }

    @Override
    public void eof() {

    }

    @Override
    public void close() {
        progressBar.close();
    }

}

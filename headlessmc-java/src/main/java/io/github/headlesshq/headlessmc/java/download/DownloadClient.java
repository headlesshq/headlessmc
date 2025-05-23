package io.github.headlesshq.headlessmc.java.download;

import io.github.headlesshq.headlessmc.api.command.ProgressBarProvider;

import java.io.IOException;
import java.nio.file.Path;

public interface DownloadClient {
    String httpGetText(String url) throws IOException;

    void downloadBigFile(String url, Path destination, String progressBarTitle, ProgressBarProvider progressBarProvider) throws IOException;

}

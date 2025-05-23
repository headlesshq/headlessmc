package io.github.headlesshq.headlessmc.java.download;

import io.github.headlesshq.headlessmc.api.command.ProgressBarProvider;
import io.github.headlesshq.headlessmc.graalvm.Java11DownloadClient;
import io.github.headlesshq.headlessmc.jline.JlineProgressbarProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

// TODO: spin up simple http server and serve some files to check all download stuff?
@Disabled("downloads stuff")
public class Java11DownloadClientTest {
    @Test
    @Disabled("downloads stuff")
    public void testDownload() throws IOException {
        ProgressBarProvider progressBarProvider = new JlineProgressbarProvider();
        DownloadClient client = new Java11DownloadClient();
        String fileUrl = "https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u442-b06/OpenJDK8U-jdk_x64_mac_hotspot_8u442b06.tar.gz";
        client.downloadBigFile(fileUrl, Paths.get("test.tar.gz"), "Test", progressBarProvider);
    }

}

package io.github.headlesshq.headlessmc.java.download;

import io.github.headlesshq.headlessmc.api.command.ProgressBarProvider;
import io.github.headlesshq.headlessmc.api.config.Config;
import io.github.headlesshq.headlessmc.api.config.ConfigImpl;
import io.github.headlesshq.headlessmc.graalvm.Java11DownloadClient;
import io.github.headlesshq.headlessmc.jline.JlineProgressbarProvider;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.os.OSFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

// TODO: spin up simple http server and serve some files to check all download stuff?
public class TemurinDownloaderTest {
    @Test
    @Disabled("requests api")
    public void testGetPackages() throws IOException {
        DownloadClient httpClient = new Java11DownloadClient();
        TemurinDownloader temurinDownloader = new TemurinDownloader();

        Config dummyConfig = new ConfigImpl(new Properties(), "dummy", 0);
        OS os = OSFactory.detect(dummyConfig);
        JavaDownloadRequest request = new JavaDownloadRequest(httpClient, ProgressBarProvider.dummy(), 8, "temurin", os, true);

        TemurinDownloader.TemurinPackage temurinPackage = temurinDownloader.getPackage(request);
        assertEquals("jdk", temurinPackage.getImageType());

        os = new OS("windows", OS.Type.WINDOWS, "11", "x64", true);
        request = new JavaDownloadRequest(httpClient, ProgressBarProvider.dummy(), 8, "temurin", os, true);
        temurinPackage = temurinDownloader.getPackage(request);
        assertEquals("jdk", temurinPackage.getImageType());
        System.out.println(temurinPackage);

        os = new OS("mac", OS.Type.OSX, "??", "x64", true);
        request = new JavaDownloadRequest(httpClient, ProgressBarProvider.dummy(), 8, "temurin", os, true);
        temurinPackage = temurinDownloader.getPackage(request);
        assertEquals("jdk", temurinPackage.getImageType());
        System.out.println(temurinPackage);
    }

    @Test
    @Disabled("downloads stuff")
    public void testDownloadWindows() throws IOException {
        DownloadClient httpClient = new Java11DownloadClient();
        TemurinDownloader temurinDownloader = new TemurinDownloader();
        OS os = new OS("windows", OS.Type.WINDOWS, "11", "x64", true);
        JavaDownloadRequest request = new JavaDownloadRequest(httpClient, new JlineProgressbarProvider(), 8, "temurin", os, false);
        temurinDownloader.download(request);
    }

    @Test
    @Disabled("downloads stuff")
    public void testDownloadTar() throws IOException {
        DownloadClient httpClient = new Java11DownloadClient();
        TemurinDownloader temurinDownloader = new TemurinDownloader();
        OS os = new OS("ubuntu", OS.Type.LINUX, "24.0.1", "x64", true);
        JavaDownloadRequest request = new JavaDownloadRequest(httpClient, new JlineProgressbarProvider(), 8, "temurin", os, false);
        temurinDownloader.download(request);
    }

}

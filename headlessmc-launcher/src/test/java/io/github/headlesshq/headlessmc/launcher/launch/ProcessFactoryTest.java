package io.github.headlesshq.headlessmc.launcher.launch;

import io.github.headlesshq.headlessmc.launcher.LauncherMock;
import io.github.headlesshq.headlessmc.launcher.download.MockDownloadService;
import io.github.headlesshq.headlessmc.os.OS;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProcessFactoryTest {
    @Test
    @SneakyThrows
    public void testCheckZipIntact() {
        val launcher = LauncherMock.INSTANCE;
        val processFactory = new ProcessFactory(
            new MockDownloadService(),
            launcher.getLauncherConfig(),
            new OS("win", OS.Type.WINDOWS, "10", true));

        File invalid = new File("DOES_NOT_EXIST!!!!");
        assertTrue(processFactory.checkZipIntact(invalid));

        invalid = new File("DOES_NOT_EXIST!!!!.jar");
        assertFalse(processFactory.checkZipIntact(invalid));

        invalid = new File("DOES_NOT_EXIST!!!!.zip");
        assertFalse(processFactory.checkZipIntact(invalid));

        invalid = LauncherMock.INSTANCE.getFileManager().create("invalid.zip");
        assertFalse(processFactory.checkZipIntact(invalid));

        invalid = LauncherMock.INSTANCE.getFileManager().create("invalid.jar");
        assertFalse(processFactory.checkZipIntact(invalid));

        val valid = launcher.getFileManager().get(false, false, "valid.zip");
        try (val zos = new ZipOutputStream(new FileOutputStream(valid));
             val fis = new FileInputStream(invalid)) {
            val zipEntry = new ZipEntry(invalid.getAbsolutePath());
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }

            zos.closeEntry();
        }

        assertTrue(processFactory.checkZipIntact(valid));
    }

}

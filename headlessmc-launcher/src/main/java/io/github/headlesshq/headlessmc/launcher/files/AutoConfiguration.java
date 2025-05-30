package io.github.headlesshq.headlessmc.launcher.files;

import lombok.CustomLog;
import io.github.headlesshq.headlessmc.api.settings.Config;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaScanner;
import io.github.headlesshq.headlessmc.java.JavaVersionFinder;
import io.github.headlesshq.headlessmc.java.JavaVersionParser;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.os.OSFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

// TODO
@CustomLog
public class AutoConfiguration {
    public static void runAutoConfiguration(Path fileManager) {
        runAutoConfiguration(fileManager, new JavaVersionFinder());
    }

    public static void runAutoConfiguration(Path fileManager, JavaVersionFinder javaVersionFinder) {
        Config dummyConfig = new ConfigImpl(new Properties(), "dummy", 0);
        if (dummyConfig.get(LauncherProperties.NO_AUTO_CONFIG, false)) {
            return;
        }

        if (!fileManager.get(false, false, "config.properties").exists()) {
            File file = fileManager.create("config.properties");
            OS os = OSFactory.detect(dummyConfig);
            boolean addFilePermissions = os.getType() == OS.Type.LINUX || os.getType() == OS.Type.OSX;
            addFilePermissions &= dummyConfig.get(LauncherProperties.JAVA_ALWAYS_ADD_FILE_PERMISSIONS, false);
            List<Java> javas = javaVersionFinder.findJavaVersions(JavaScanner.of(new JavaVersionParser(addFilePermissions)), os);
            try (OutputStream output = Files.newOutputStream(file.toPath())) {
                output.write("hmc.java.versions=".getBytes(StandardCharsets.UTF_8));
                Iterator<Java> itr = javas.iterator();
                while (itr.hasNext()) {
                    Java java = itr.next();
                    // ignore javas in HeadlessMC/java, these get scanned anyway
                    if (Paths.get(java.getExecutable()).toAbsolutePath().startsWith(fileManager.getBase().toPath().toAbsolutePath())) {
                        continue;
                    }

                    output.write(java.getExecutable().replace("\\", "/").getBytes(StandardCharsets.UTF_8));
                    if (itr.hasNext()) {
                        output.write(";".getBytes(StandardCharsets.UTF_8));
                    }
                }
            } catch (IOException e) {
                log.error(e);
            }
        }
    }

}

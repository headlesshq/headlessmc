package io.github.headlesshq.headlessmc.java;

import lombok.CustomLog;
import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.os.OS;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@CustomLog
public class JavaVersionFinder {
    public List<Java> findJavaVersions(JavaScanner javaScanner, OS os) {
        List<Java> result = new ArrayList<>();
        checkDirectory(javaScanner, Paths.get(HeadlessMc.NAME).resolve("java"), os);
        if (os.getType() == OS.Type.WINDOWS) {
            Iterable<Path> rootDirectories = FileSystems.getDefault().getRootDirectories();
            for (Path rootPath : rootDirectories) {
                result.addAll(checkDirectory(javaScanner, rootPath.resolve("Program Files").resolve("Java"), os));
            }

            // search Users/<user>/.jdks?
        } else if (os.getType() == OS.Type.LINUX) {
            Iterable<Path> rootDirectories = FileSystems.getDefault().getRootDirectories();
            for (Path rootPath : rootDirectories) {
                result.addAll(checkDirectory(javaScanner, rootPath.resolve("usr").resolve("lib").resolve("jvm"), os));
                result.addAll(checkDirectory(javaScanner, rootPath.resolve("usr").resolve("local"), os));
            }
        }

        result.sort(Comparator.naturalOrder());
        return result;
    }

    public List<Java> checkDirectory(JavaScanner javaScanner, Path javaDirPath, OS os) {
        return checkDirectory(javaScanner, javaDirPath, os, Collections.emptyList());
    }

    public List<Java> checkDirectory(JavaScanner javaScanner, Path javaDirPath, OS os, Collection<Java> alreadyIn) {
        if (Files.exists(javaDirPath) && Files.isDirectory(javaDirPath)) {
            try (Stream<Path> stream = Files.list(javaDirPath)) {
                List<Java> result = new ArrayList<>();
                stream.forEach(javaVersion -> {
                    Path executable = javaVersion.resolve("bin").resolve(os.getType() == OS.Type.WINDOWS ? "java.exe" : "java");
                    if (Files.exists(executable) && !Files.isDirectory(executable)) {
                        String path = executable.toAbsolutePath().toString();
                        if (path.toLowerCase(Locale.ENGLISH).endsWith(".exe")) {
                            path = path.substring(0, path.length() - 4);
                        }

                        String replaced = path.replace("\\", "/");
                        if (!alreadyIn.stream().anyMatch(java -> replaced.equals(java.getExecutable()))) {
                            Java java = javaScanner.scanJava(log, path);
                            if (java != null) {
                                result.add(java);
                            }
                        }
                    }
                });

                return result;
            } catch (IOException e) {
                log.error("Failed to list files in " + javaDirPath + " : " + e.getMessage());
            }
        }

        return Collections.emptyList();
    }

}

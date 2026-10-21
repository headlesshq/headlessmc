package io.github.headlesshq.headlessmc.java.inmemory;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaService;
import io.github.headlesshq.headlessmc.java.args.ArgPair;
import io.github.headlesshq.headlessmc.java.args.SystemPropertyUtil;
import io.github.headlesshq.headlessmc.java.launcher.AbstractJavaProcessBuilder;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcess;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Instead of starting a process, loads the classpath onto a {@link URLClassLoader}
 * and runs the main class in a new thread of this JVM, like the InMemoryLauncher of HeadlessMc 2.
 * <ul>
 *     <li>System properties ({@code -D} jvm args included) are set globally for this JVM.</li>
 *     <li>Other jvm args, like {@code -Xmx}, cannot be applied and are ignored.</li>
 *     <li>The working directory of this JVM cannot be changed, the directory is only created.</li>
 *     <li>A program calling {@link System#exit(int)} exits this JVM.</li>
 * </ul>
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class InMemoryProcessBuilder extends AbstractJavaProcessBuilder {
    private static final List<String> CLASSPATH_ARGS = List.of("-cp", "-classpath", "--class-path");

    private final JavaService javaService;

    @Override
    public JavaProcess start() throws HeadlessMcException {
        List<String> classpath = new ArrayList<>(getClassPath());
        Map<String, @Nullable String> systemProperties = new LinkedHashMap<>();
        parseJvmArgs(classpath, systemProperties);
        systemProperties.putAll(systemPropertiesToMap());

        String mainClass = getMainClass();
        Path jar = getJar();
        if (jar != null) {
            // like java -jar, the jar and its manifest Class-Path replace the classpath
            classpath = new ArrayList<>();
            mainClass = readManifest(jar, classpath);
        } else if (mainClass == null) {
            throw new IllegalStateException("Neither jar nor main-class were specified");
        }

        if (isPipeIO()) {
            log.warn("{}: in-memory processes share System.in and System.out with HeadlessMc", getId());
        }

        Path directory = getDirectory();
        if (directory != null) {
            try {
                Files.createDirectories(directory.toAbsolutePath());
            } catch (IOException e) {
                throw new JavaProcessException(e);
            }
        }

        systemProperties.forEach((key, value) -> System.setProperty(key, value == null ? "" : value));
        System.setProperty("java.class.path", String.join(File.pathSeparator, classpath));
        URLClassLoader classLoader = new URLClassLoader(getId(), toUrls(classpath), ClassLoader.getPlatformClassLoader());
        Java java = findJava();
        InMemoryJavaProcess process = new InMemoryJavaProcess(
            getId(),
            systemProperties,
            classpath,
            new ArrayList<>(getJvmArgs()),
            new ArrayList<>(getArgs()),
            Optional.ofNullable(java),
            Optional.ofNullable(java == null ? getVersion() : Integer.valueOf(java.version())),
            Optional.of(mainClass),
            Optional.ofNullable(directory),
            Optional.ofNullable(jar),
            classLoader
        );

        process.start(mainClass, getArgs().toArray(new String[0]));
        return process;
    }

    private void parseJvmArgs(List<String> classpath, Map<String, @Nullable String> systemProperties) {
        Iterator<String> iterator = getJvmArgs().iterator();
        while (iterator.hasNext()) {
            String arg = iterator.next();
            if (SystemPropertyUtil.isSystemProperty(arg)) {
                ArgPair pair = SystemPropertyUtil.parseSystemProperty(arg);
                systemProperties.put(pair.arg(), pair.value());
            } else if (CLASSPATH_ARGS.contains(arg) && iterator.hasNext()) {
                classpath.addAll(Arrays.asList(iterator.next().split(File.pathSeparator)));
            } else {
                log.warn("{}: ignoring jvm arg {}, it cannot be applied in-memory", getId(), arg);
            }
        }
    }

    private String readManifest(Path jar, List<String> classpath) {
        classpath.add(jar.toAbsolutePath().toString());
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            Manifest manifest = jarFile.getManifest();
            String mainClass = manifest == null ? null : manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
            if (mainClass == null) {
                throw new JavaProcessException("No Main-Class in manifest of " + jar);
            }

            String manifestClasspath = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
            if (manifestClasspath != null) {
                Path parent = jar.toAbsolutePath().getParent();
                for (String entry : manifestClasspath.trim().split("\\s+")) {
                    if (!entry.isEmpty()) {
                        classpath.add(parent == null ? entry : parent.resolve(entry).toString());
                    }
                }
            }

            return mainClass;
        } catch (IOException e) {
            throw new JavaProcessException("Failed to read manifest of " + jar, e);
        }
    }

    private @Nullable Java findJava() {
        Java java = getJava();
        if (java != null) {
            return java;
        }

        Integer version = getVersion();
        return version == null ? null : javaService.getJava(version).orElse(null);
    }

    private static URL[] toUrls(List<String> classpath) {
        URL[] urls = new URL[classpath.size()];
        for (int i = 0; i < classpath.size(); i++) {
            try {
                urls[i] = Paths.get(classpath.get(i)).toUri().toURL();
            } catch (MalformedURLException e) {
                throw new JavaProcessException("Invalid classpath entry " + classpath.get(i), e);
            }
        }

        return urls;
    }

}

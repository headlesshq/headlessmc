package io.github.headlesshq.headlessmc.java.sources;

import io.github.headlesshq.headlessmc.os.OS;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;

/**
 * Finds the java executable in JAVA_HOME.
 * E.g. JAVA_HOME/bin/java or JAVA_HOME/bin/java.exe on Windows.
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class JavaExecutableFinder {
    private final OS os;

    /**
     * Finds the java executable in JAVA_HOME.
     * E.g. JAVA_HOME/bin/java or JAVA_HOME/bin/java.exe on Windows.
     *
     * @param javaHome the Java home to find the java executable in.
     * @return the java executable in JAVA_HOME.
     */
    public Path getExecutable(Path javaHome) {
        return javaHome.resolve("bin").resolve(OS.Type.WINDOWS.equals(os.type()) ? "java.exe" : "java");
    }

}

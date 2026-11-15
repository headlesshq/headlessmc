package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.launcher.profile.PathCandidates;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * The {@value #NAME} launch target: launches a jar with {@code java -jar}, e.g. a server jar
 * that has been downloaded by hand.
 *
 * <p>Arguments: {@code [client|server] <path/to/file.jar>}, e.g. {@code server ./paper.jar}.
 * The file has to be an existing jar with a {@code Main-Class}, anything else cannot be launched this way.
 * See {@link AbstractExecutableLaunchTarget} for the side and the game directory of such a profile.</p>
 *
 * <p>A profile without a Java version gets the Java version the main class of the jar has been compiled for.</p>
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class JarLaunchTarget extends AbstractExecutableLaunchTarget<JarLaunchTarget.Handle> {
    /**
     * The name of this launch target.
     */
    public static final String NAME = "jar";
    private static final String JAR = ".jar";
    private static final int CLASS_FILE_MAGIC = 0xCAFEBABE;
    /** The class file version of Java 1 was 45, so the class file version of Java n is n + 44. */
    private static final int CLASS_FILE_VERSION_OFFSET = 44;
    private static final int MIN_JAVA_VERSION = 8;

    private final ServerLauncher serverLauncher;
    private final FileService fileService;
    private final JsonService jsonService;

    @Override
    protected JsonService jsonService() {
        return jsonService;
    }

    @Override
    protected FileService fileService() {
        return fileService;
    }

    @Override
    protected ServerLauncher serverLauncher() {
        return serverLauncher;
    }

    @Override
    protected Class<Handle> handleType() {
        return Handle.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected Path getFile(Handle handle) {
        return handle.jar();
    }

    @Override
    protected Handle newHandle(Path file) {
        return new Handle(file);
    }

    @Override
    protected Predicate<Path> filter() {
        return PathCandidates.extension(JAR);
    }

    @Override
    protected void verify(Path file) throws IllegalArgumentException {
        if (!PathCandidates.hasExtension(file, JAR)) {
            throw new IllegalArgumentException(file + " is not a .jar file");
        }

        try (JarFile jar = new JarFile(file.toFile())) {
            if (getMainClass(jar) == null) {
                throw new IllegalArgumentException(file + " has no Main-Class, it cannot be launched with java -jar");
            }
        } catch (IOException | UnsupportedOperationException e) {
            throw new IllegalArgumentException(file + " is not a jar: " + e.getMessage(), e);
        }
    }

    /**
     * @return the Java version the main class of the jar has been compiled for, at least Java 8,
     * the Java version HeadlessMc runs on if it cannot be read.
     */
    @Override
    protected int defaultJavaVersion(Path file) {
        try (JarFile jar = new JarFile(file.toFile())) {
            @Nullable String mainClass = getMainClass(jar);
            @Nullable JarEntry entry = mainClass == null ? null : jar.getJarEntry(mainClass.replace('.', '/') + ".class");
            if (entry != null) {
                try (InputStream inputStream = jar.getInputStream(entry);
                     DataInputStream data = new DataInputStream(inputStream)) {
                    if (data.readInt() == CLASS_FILE_MAGIC) {
                        data.readUnsignedShort(); // minor version
                        return Math.max(MIN_JAVA_VERSION, data.readUnsignedShort() - CLASS_FILE_VERSION_OFFSET);
                    }
                }
            }
        } catch (IOException | UnsupportedOperationException e) {
            log.debug("Failed to read the Java version of {}", file, e);
        }

        return Runtime.version().feature();
    }

    private static @Nullable String getMainClass(JarFile jar) throws IOException {
        @Nullable Manifest manifest = jar.getManifest();
        return manifest == null ? null : manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
    }

    /**
     * The data of the {@value #NAME} launch target.
     *
     * @param jar the jar that is launched.
     */
    @RegisterForReflection
    public record Handle(Path jar) implements ReflectionRegistered {
        @SuppressWarnings("ConstantValue")
        public Handle {
            Objects.requireNonNull(jar, "Launch target data is missing 'jar'");
        }
    }

}

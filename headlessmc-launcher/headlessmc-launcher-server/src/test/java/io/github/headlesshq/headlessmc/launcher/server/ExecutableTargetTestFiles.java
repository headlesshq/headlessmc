package io.github.headlesshq.headlessmc.launcher.server;

import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Creates the files the {@link JarLaunchTarget} and {@link ScriptLaunchTarget} launch.
 */
@UtilityClass
class ExecutableTargetTestFiles {
    static final String MAIN_CLASS = "test.Main";

    /**
     * Writes a jar.
     *
     * @param file        the jar to write.
     * @param mainClass   the {@code Main-Class} of its manifest, {@code null} for a jar that cannot be launched.
     * @param javaVersion the Java version the main class has been compiled for, {@code null} to leave the class out.
     * @return the absolute, normalized path of the jar.
     */
    static Path jar(Path file, @Nullable String mainClass, @Nullable Integer javaVersion) {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (mainClass != null) {
            manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass);
        }

        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            try (OutputStream out = Files.newOutputStream(file); JarOutputStream jar = new JarOutputStream(out, manifest)) {
                if (mainClass != null && javaVersion != null) {
                    jar.putNextEntry(new JarEntry(mainClass.replace('.', '/') + ".class"));
                    // the header of a class file: magic, minor version, major version
                    jar.write(ByteBuffer.allocate(8).putInt(0xCAFEBABE).putShort((short) 0)
                                  .putShort((short) (javaVersion + 44)).array());
                    jar.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return file.toAbsolutePath().normalize();
    }

    static Path file(Path file, String content) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            return Files.writeString(file, content).toAbsolutePath().normalize();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}

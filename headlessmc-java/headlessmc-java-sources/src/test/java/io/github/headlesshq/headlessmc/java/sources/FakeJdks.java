package io.github.headlesshq.headlessmc.java.sources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * Creates fake JDK directories whose bin/java is a shell script
 * printing a version banner to stderr, like a real java -version.
 */
final class FakeJdks {
    private FakeJdks() {
    }

    static Path create(Path dir, int version) throws IOException {
        return withScript(dir, "#!/bin/sh\necho 'openjdk version \"" + version + ".0.1\" 2021-10-19' >&2\n");
    }

    static Path createBroken(Path dir) throws IOException {
        return withScript(dir, "#!/bin/sh\necho 'no version here' >&2\n");
    }

    private static Path withScript(Path dir, String script) throws IOException {
        Path bin = dir.resolve("bin");
        Files.createDirectories(bin);
        Path java = bin.resolve("java");
        Files.writeString(java, script);
        Files.setPosixFilePermissions(java, PosixFilePermissions.fromString("rwxr-xr-x"));
        return dir;
    }

}

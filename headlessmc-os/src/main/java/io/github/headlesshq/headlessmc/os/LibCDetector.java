package io.github.headlesshq.headlessmc.os;

import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Detects the {@link LibC} of the Linux system and of the running process.
 * Both can differ, e.g. the fully static musl executable of HeadlessMc does not use the C library of the system.
 */
public final class LibCDetector {
    private static final Path PROC_SELF_MAPS = Path.of("/proc/self/maps");
    private static final Path SHELL = Path.of("/bin/sh");
    private static final Path LIB = Path.of("/lib");
    private static final int PT_INTERP = 3;
    private static final int MAX_INTERPRETER_LENGTH = 4096;

    /*
     * Computed lazily at run time instead of in a static initializer,
     * so that a native image never contains the C library of the machine it has been built on.
     */
    private static volatile @Nullable LibC system;
    private static volatile @Nullable LibC process;

    private LibCDetector() {
        throw new AssertionError();
    }

    /**
     * Detects the C library of the Linux system, which e.g. Java runtimes have to be built against.
     * Uses the C library of the running process, if it is dynamically linked,
     * otherwise the dynamic loader of {@code /bin/sh}, which is the loader of the C library of the system.
     * If {@code /bin/sh} is statically linked, too, the system is considered musl based if a musl loader is installed.
     *
     * The result is cached, because the C library cannot change while the process runs.
     *
     * @return the C library of the system, {@link LibC#UNKNOWN} if it cannot be detected, e.g. on other systems.
     */
    public static LibC detectSystem() {
        LibC result = system;
        if (result == null) {
            result = detectSystem(readMaps(), readInterpreterOrNull(SHELL), isMuslLoaderInstalled());
            system = result;
        }

        return result;
    }

    /**
     * Detects the C library the running process uses.
     * A process that maps no C library is a fully static executable,
     * which GraalVM can only build against musl.
     * The result is cached, because the C library cannot change while the process runs.
     *
     * @return the C library of the running process, {@link LibC#UNKNOWN} if it cannot be detected,
     * e.g. on other systems than Linux.
     */
    public static LibC detectProcess() {
        LibC result = process;
        if (result == null) {
            result = detectProcess(readMaps());
            process = result;
        }

        return result;
    }

    @VisibleForTesting
    static LibC detectSystem(@Nullable String maps, @Nullable String shellInterpreter, boolean muslLoaderInstalled) {
        LibC process = maps == null ? LibC.UNKNOWN : fromMaps(maps);
        if (process != LibC.UNKNOWN) {
            return process;
        }

        LibC shell = shellInterpreter == null ? LibC.UNKNOWN : fromLoader(shellInterpreter);
        if (shell != LibC.UNKNOWN) {
            return shell;
        }

        return muslLoaderInstalled ? LibC.MUSL : LibC.UNKNOWN;
    }

    @VisibleForTesting
    static LibC detectProcess(@Nullable String maps) {
        if (maps == null || maps.isBlank()) {
            return LibC.UNKNOWN;
        }

        LibC libC = fromMaps(maps);
        return libC == LibC.UNKNOWN ? LibC.MUSL : libC;
    }

    /**
     * Detects the C library from the memory mappings of a process, as listed in {@code /proc/<pid>/maps}.
     * Musl wins over glibc, because gcompat provides a {@code libc.so.6} on musl systems.
     *
     * @param maps the contents of {@code /proc/<pid>/maps}.
     * @return the C library mapped by the process, or {@link LibC#UNKNOWN} if it maps none.
     */
    @VisibleForTesting
    static LibC fromMaps(String maps) {
        boolean glibc = false;
        for (String line : maps.split("\n")) {
            int path = line.indexOf('/');
            if (path < 0) {
                continue;
            }

            String fileName = fileName(line.substring(path).strip());
            if (isMuslLoader(fileName) || fileName.startsWith("libc.musl-")) {
                return LibC.MUSL;
            }

            if (isGlibcLoader(fileName) || "libc.so.6".equals(fileName)) {
                glibc = true;
            }
        }

        return glibc ? LibC.GLIBC : LibC.UNKNOWN;
    }

    /**
     * Detects the C library from the path of a dynamic loader, the interpreter of an ELF executable.
     *
     * @param loader the path of the dynamic loader, e.g. {@code /lib64/ld-linux-x86-64.so.2}.
     * @return the C library the loader belongs to, or {@link LibC#UNKNOWN}.
     */
    @VisibleForTesting
    static LibC fromLoader(String loader) {
        String fileName = fileName(loader.strip());
        if (isMuslLoader(fileName)) {
            return LibC.MUSL;
        }

        return isGlibcLoader(fileName) ? LibC.GLIBC : LibC.UNKNOWN;
    }

    /**
     * Reads the interpreter, the dynamic loader, of an ELF executable.
     *
     * @param elf the executable.
     * @return the path of the dynamic loader, or {@code null} if the executable is statically linked.
     * @throws IOException if the file cannot be read or is not an ELF file.
     */
    @VisibleForTesting
    static @Nullable String readInterpreter(Path elf) throws IOException {
        try (FileChannel channel = FileChannel.open(elf, StandardOpenOption.READ)) {
            ByteBuffer ident = read(channel, 0, 16);
            if (ident.getInt(0) != 0x7F454C46) {
                throw new IOException(elf + " is not an ELF file");
            }

            boolean is64 = ident.get(4) == 2;
            ByteOrder order = ident.get(5) == 2 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
            ByteBuffer header = read(channel, 0, is64 ? 64 : 52).order(order);
            long programHeaders = is64 ? header.getLong(0x20) : Integer.toUnsignedLong(header.getInt(0x1C));
            int entrySize = Short.toUnsignedInt(header.getShort(is64 ? 0x36 : 0x2A));
            int entries = Short.toUnsignedInt(header.getShort(is64 ? 0x38 : 0x2C));
            for (int i = 0; i < entries; i++) {
                ByteBuffer entry = read(channel, programHeaders + (long) i * entrySize, entrySize).order(order);
                if (entry.getInt(0) != PT_INTERP) {
                    continue;
                }

                long offset = is64 ? entry.getLong(0x08) : Integer.toUnsignedLong(entry.getInt(0x04));
                long size = is64 ? entry.getLong(0x20) : Integer.toUnsignedLong(entry.getInt(0x10));
                if (size <= 0 || size > MAX_INTERPRETER_LENGTH) {
                    throw new IOException("Invalid interpreter size " + size + " in " + elf);
                }

                ByteBuffer interpreter = read(channel, offset, (int) size);
                String result = new String(interpreter.array(), StandardCharsets.UTF_8);
                int terminator = result.indexOf('\0');
                return terminator < 0 ? result : result.substring(0, terminator);
            }

            return null;
        }
    }

    private static ByteBuffer read(FileChannel channel, long position, int length) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        while (buffer.hasRemaining()) {
            if (channel.read(buffer, position + buffer.position()) < 0) {
                throw new IOException("Unexpected end of file");
            }
        }

        return buffer.flip();
    }

    private static boolean isMuslLoader(String fileName) {
        return fileName.startsWith("ld-musl-");
    }

    private static boolean isGlibcLoader(String fileName) {
        // ld-linux-x86-64.so.2, ld-linux-aarch64.so.1, ld-linux.so.2, and ld64.so.1/2 on ppc64 and s390x
        return fileName.startsWith("ld-linux") || fileName.startsWith("ld64.so.");
    }

    private static String fileName(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    private static @Nullable String readMaps() {
        try {
            return Files.readString(PROC_SELF_MAPS);
        } catch (IOException | UncheckedIOException | SecurityException e) {
            return null;
        }
    }

    private static @Nullable String readInterpreterOrNull(Path elf) {
        try {
            return readInterpreter(elf);
        } catch (IOException | SecurityException e) {
            return null;
        }
    }

    private static boolean isMuslLoaderInstalled() {
        try (DirectoryStream<Path> loaders = Files.newDirectoryStream(LIB, "ld-musl-*.so.1")) {
            return loaders.iterator().hasNext();
        } catch (IOException | UncheckedIOException | SecurityException e) {
            return false;
        }
    }

}

package io.github.headlesshq.headlessmc.os;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class LibCDetectorTest {
    private final LibCDetector detector = new LibCDetector();

    private static final String GLIBC_MAPS = """
        5581c6a00000-5581c6a02000 r--p 00000000 103:02 1234 /usr/bin/java
        7f2b1c000000-7f2b1c028000 r--p 00000000 103:02 5678 /usr/lib/x86_64-linux-gnu/libc.so.6
        7f2b1c400000-7f2b1c402000 r--p 00000000 103:02 9012 /usr/lib/x86_64-linux-gnu/ld-linux-x86-64.so.2
        7ffd5a1f0000-7ffd5a211000 rw-p 00000000 00:00 0 [stack]
        """;
    private static final String MUSL_MAPS = """
        5581c6a00000-5581c6a02000 r--p 00000000 103:02 1234 /usr/lib/jvm/java-21-openjdk/bin/java
        7f2b1c000000-7f2b1c028000 r--p 00000000 103:02 5678 /lib/libgcompat.so.0
        7f2b1c100000-7f2b1c128000 r--p 00000000 103:02 5679 /lib/libc.so.6
        7f2b1c400000-7f2b1c402000 r--p 00000000 103:02 9012 /lib/ld-musl-x86_64.so.1
        """;
    private static final String STATIC_MAPS = """
        00400000-00401000 r--p 00000000 103:02 1234 /home/user/.local/bin/headlessmc
        01d3f000-01d60000 rw-p 00000000 00:00 0 [heap]
        7ffd5a1f0000-7ffd5a211000 rw-p 00000000 00:00 0 [stack]
        7ffd5a3f0000-7ffd5a3f2000 r-xp 00000000 00:00 0 [vdso]
        """;

    @Test
    public void testFromMaps() {
        assertEquals(LibC.GLIBC, detector.fromMaps(GLIBC_MAPS));
        assertEquals(LibC.MUSL, detector.fromMaps(MUSL_MAPS));
        assertEquals(LibC.UNKNOWN, detector.fromMaps(STATIC_MAPS));
        assertEquals(LibC.UNKNOWN, detector.fromMaps(""));
    }

    @Test
    public void testFromLoader() {
        assertEquals(LibC.MUSL, detector.fromLoader("/lib/ld-musl-x86_64.so.1"));
        assertEquals(LibC.MUSL, detector.fromLoader("/lib/ld-musl-aarch64.so.1"));
        assertEquals(LibC.GLIBC, detector.fromLoader("/lib64/ld-linux-x86-64.so.2"));
        assertEquals(LibC.GLIBC, detector.fromLoader("/lib/ld-linux-aarch64.so.1"));
        assertEquals(LibC.GLIBC, detector.fromLoader("/lib/ld-linux.so.2"));
        assertEquals(LibC.GLIBC, detector.fromLoader("/lib64/ld64.so.2"));
        assertEquals(LibC.GLIBC, detector.fromLoader("/nix/store/abc-glibc-2.40-66/lib/ld-linux-x86-64.so.2"));
        assertEquals(LibC.UNKNOWN, detector.fromLoader("/system/bin/linker64"));
    }

    @Test
    public void testDetectProcess() {
        assertEquals(LibC.GLIBC, detector.detectProcess(GLIBC_MAPS));
        assertEquals(LibC.MUSL, detector.detectProcess(MUSL_MAPS));
        assertEquals(LibC.MUSL, detector.detectProcess(STATIC_MAPS));
        assertEquals(LibC.UNKNOWN, detector.detectProcess(null));
        assertEquals(LibC.UNKNOWN, detector.detectProcess(""));
    }

    @Test
    public void testDetectSystem() {
        assertEquals(LibC.GLIBC, detector.detectSystem(GLIBC_MAPS, "/lib/ld-musl-x86_64.so.1", true));
        assertEquals(LibC.MUSL, detector.detectSystem(MUSL_MAPS, null, false));
        assertEquals(LibC.GLIBC, detector.detectSystem(STATIC_MAPS, "/nix/store/abc-glibc/lib/ld-linux-x86-64.so.2", false));
        assertEquals(LibC.MUSL, detector.detectSystem(STATIC_MAPS, "/lib/ld-musl-x86_64.so.1", false));
        assertEquals(LibC.MUSL, detector.detectSystem(STATIC_MAPS, null, true));
        assertEquals(LibC.UNKNOWN, detector.detectSystem(STATIC_MAPS, null, false));
        assertEquals(LibC.UNKNOWN, detector.detectSystem(null, null, false));
    }

    @Test
    public void testReadInterpreter64BitLittleEndian(@TempDir Path dir) throws IOException {
        Path elf = dir.resolve("elf64");
        Files.write(elf, elf(true, ByteOrder.LITTLE_ENDIAN, "/lib/ld-musl-x86_64.so.1"));
        assertEquals("/lib/ld-musl-x86_64.so.1", detector.readInterpreter(elf));
    }

    @Test
    public void testReadInterpreter32BitBigEndian(@TempDir Path dir) throws IOException {
        Path elf = dir.resolve("elf32");
        Files.write(elf, elf(false, ByteOrder.BIG_ENDIAN, "/lib/ld.so.1"));
        assertEquals("/lib/ld.so.1", detector.readInterpreter(elf));
    }

    @Test
    public void testReadInterpreterStatic(@TempDir Path dir) throws IOException {
        Path elf = dir.resolve("static");
        Files.write(elf, elf(true, ByteOrder.LITTLE_ENDIAN, null));
        assertNull(detector.readInterpreter(elf));
    }

    @Test
    public void testReadInterpreterNoElf(@TempDir Path dir) throws IOException {
        Path script = dir.resolve("script");
        Files.writeString(script, "#!/bin/sh\necho this is not an ELF file\n");
        assertThrows(IOException.class, () -> detector.readInterpreter(script));
    }

    /**
     * Creates a minimal ELF file with a {@code PT_LOAD} program header
     * and a {@code PT_INTERP} program header, if an interpreter is given.
     */
    private static byte[] elf(boolean is64, ByteOrder order, String interpreter) {
        int headerSize = is64 ? 64 : 52;
        int entrySize = is64 ? 56 : 32;
        int entries = interpreter == null ? 1 : 2;
        int interpreterOffset = headerSize + entries * entrySize;
        byte[] interpreterBytes = interpreter == null
            ? new byte[0]
            : (interpreter + "\0").getBytes(StandardCharsets.US_ASCII);
        ByteBuffer buffer = ByteBuffer.allocate(interpreterOffset + interpreterBytes.length).order(order);
        buffer.put(new byte[] {0x7F, 'E', 'L', 'F', (byte) (is64 ? 2 : 1), (byte) (order == ByteOrder.BIG_ENDIAN ? 2 : 1), 1});
        if (is64) {
            buffer.putLong(0x20, headerSize);
            buffer.putShort(0x36, (short) entrySize);
            buffer.putShort(0x38, (short) entries);
        } else {
            buffer.putInt(0x1C, headerSize);
            buffer.putShort(0x2A, (short) entrySize);
            buffer.putShort(0x2C, (short) entries);
        }

        buffer.putInt(headerSize, 1);
        if (interpreter != null) {
            int entry = headerSize + entrySize;
            buffer.putInt(entry, 3);
            if (is64) {
                buffer.putLong(entry + 0x08, interpreterOffset);
                buffer.putLong(entry + 0x20, interpreterBytes.length);
            } else {
                buffer.putInt(entry + 0x04, interpreterOffset);
                buffer.putInt(entry + 0x10, interpreterBytes.length);
            }

            buffer.put(interpreterOffset, interpreterBytes);
        }

        return buffer.array();
    }

}

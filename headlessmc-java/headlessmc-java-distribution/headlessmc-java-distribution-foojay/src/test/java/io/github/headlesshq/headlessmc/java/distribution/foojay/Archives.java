package io.github.headlesshq.headlessmc.java.distribution.foojay;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Builds small in-memory archives for the extractor/installer tests. */
final class Archives {
    private Archives() {
    }

    static byte[] zip(Map<String, String> entries) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(bytes)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                out.putArchiveEntry(new ZipArchiveEntry(entry.getKey()));
                if (!entry.getKey().endsWith("/")) {
                    out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                }

                out.closeArchiveEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return bytes.toByteArray();
    }

    static byte[] tarGz(Map<String, String> entries) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (OutputStream gzip = new GzipCompressorOutputStream(bytes);
             TarArchiveOutputStream out = new TarArchiveOutputStream(gzip)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                byte[] content = entry.getValue().getBytes(StandardCharsets.UTF_8);
                TarArchiveEntry tarEntry = new TarArchiveEntry(entry.getKey());
                tarEntry.setSize(entry.getKey().endsWith("/") ? 0 : content.length);
                out.putArchiveEntry(tarEntry);
                if (!entry.getKey().endsWith("/")) {
                    out.write(content);
                }

                out.closeArchiveEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return bytes.toByteArray();
    }

}

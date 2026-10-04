package io.github.headlesshq.headlessmc.net;

import io.github.headlesshq.headlessmc.exceptions.ExConsumer;
import io.github.headlesshq.headlessmc.exceptions.ExFunction;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcNoSuchAlgorithmException;
import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;
import io.github.headlesshq.headlessmc.exceptions.VerificationException;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import jakarta.enterprise.util.TypeLiteral;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class MockDownloadBuilder extends AbstractDownloadBuilder implements DownloadBuilder {
    private final MockDownloadService mockDownloadService;

    public MockDownloadBuilder(URI uri, MockDownloadService mockDownloadService) {
        super(uri);
        this.mockDownloadService = mockDownloadService;
    }

    @Override
    public DownloadBuilder retry(int retries, int initialIntervalMs, double factor, double jitter) {
        return this;
    }

    @Override
    public void start(ExConsumer<Download> action) throws HeadlessMcException {
        VerifiedDownload download = read();
        try {
            action.accept(download.asDownload());
        } catch (Exception e) {
            throw wrap(e);
        }
    }

    @Override
    public <T> T map(ExFunction<Download, T> action) throws HeadlessMcException {
        VerifiedDownload download = read();
        try {
            return action.apply(download.asDownload());
        } catch (Exception e) {
            throw wrap(e);
        }
    }

    @Override
    public <T extends ReflectionRegistered> T json(Class<T> type) throws HeadlessMcException {
        throw new UnsupportedOperationException("json parsing is not supported by MockDownloadBuilder");
    }

    @Override
    public <T extends ReflectionRegistered> T json(TypeLiteral<T> typeLiteral) throws HeadlessMcException {
        throw new UnsupportedOperationException("json parsing is not supported by MockDownloadBuilder");
    }

    @Override
    public <T> T jsonRegisteredForReflection(TypeLiteral<T> typeLiteral) throws HeadlessMcException {
        throw new UnsupportedOperationException("json parsing is not supported by MockDownloadBuilder");
    }

    @Override
    public Path toFile(Path file) throws HeadlessMcException {
        VerifiedDownload download = read();
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.write(file, download.bytes());
            return file;
        } catch (IOException e) {
            throw new HeadlessMcIOException(e);
        }
    }

    private VerifiedDownload read() throws HeadlessMcException {
        try (Download download = mockDownloadService.open(uri)) {
            int statusCode = download.getStatusCode();
            byte[] bytes = download.getInputStream().readAllBytes();
            if (statusCodeHandler != null) {
                statusCodeHandler.accept(statusCode, () -> new String(bytes, StandardCharsets.UTF_8));
            }

            verify(bytes);
            return new VerifiedDownload(statusCode, bytes);
        } catch (IOException e) {
            throw new HeadlessMcIOException(e);
        }
    }

    private void verify(byte[] bytes) throws HeadlessMcException {
        if (size != null && size != bytes.length) {
            throw new VerificationException(
                "Wrong size for " + uri + ": expected " + size + " but got " + bytes.length);
        }

        Map<MessageDigest, String> digests = new HashMap<>();
        resolvedHashAlgorithms.forEach((digest, hash) -> {
            if (hash != null) {
                digests.put(digest, hash);
            }
        });

        boolean found = hashAlgorithms.isEmpty();
        for (Map.Entry<String, String> entry : hashAlgorithms.entrySet()) {
            try {
                digests.put(MessageDigest.getInstance(normalize(entry.getKey())), entry.getValue());
                found = true;
            } catch (NoSuchAlgorithmException ignored) {
            }
        }

        if (!found && digests.isEmpty()) {
            throw new HeadlessMcNoSuchAlgorithmException("Failed to find algorithms: " + hashAlgorithms);
        }

        for (Map.Entry<MessageDigest, String> entry : digests.entrySet()) {
            String actual = toHex(entry.getKey().digest(bytes));
            if (!actual.equalsIgnoreCase(entry.getValue())) {
                throw new VerificationException("Digest mismatch for " + uri + " (" + entry.getKey().getAlgorithm()
                    + "): expected " + entry.getValue() + ", got " + actual);
            }
        }
    }

    private static String normalize(String algorithm) {
        String name = algorithm.toUpperCase(Locale.ENGLISH).replace("_", "-");
        return switch (name) {
            case "SHA1" -> "SHA-1";
            case "SHA224" -> "SHA-224";
            case "SHA256" -> "SHA-256";
            case "SHA384" -> "SHA-384";
            case "SHA512" -> "SHA-512";
            case "MD-5" -> "MD5";
            default -> name;
        };
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    private static HeadlessMcException wrap(Exception e) {
        switch (e) {
            case HeadlessMcException headlessMcException -> throw headlessMcException;
            case UncheckedInterruptedException interruptedException -> throw interruptedException;
            case InterruptedException interruptedException -> {
                Thread.currentThread().interrupt();
                throw new UncheckedInterruptedException(interruptedException);
            }
            case IOException ioException -> throw new HeadlessMcIOException(ioException);
            case RuntimeException runtimeException -> throw runtimeException;
            default -> throw new HeadlessMcException(e);
        }
    }

    private record VerifiedDownload(int statusCode, byte[] bytes) {
        Download asDownload() {
            return new DownloadImpl(
                Optional.of((long) bytes.length),
                statusCode,
                new ByteArrayInputStream(bytes)
            );
        }
    }

}

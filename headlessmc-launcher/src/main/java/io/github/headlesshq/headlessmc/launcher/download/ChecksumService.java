package io.github.headlesshq.headlessmc.launcher.download;

import io.github.headlesshq.headlessmc.launcher.util.IOConsumer;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

/**
 * A service for verifying hashes. Default implementation verifies SHA1.
 */
public class ChecksumService {
    public boolean checkIntegrity(byte[] bytes, @Nullable Long size, @Nullable String hash) {
        if (size != null && size >= 0L && size != bytes.length) {
            return false;
        }

        if (hash != null) {
            String byteHash = hash(bytes);
            return hash.equalsIgnoreCase(byteHash);
        }

        return true;
    }

    public boolean checkIntegrity(Path path, @Nullable Long size, @Nullable String hash) throws IOException {
        if (size == null && hash == null) {
            return true;
        }

        try (InputStream is = Files.newInputStream(path)) {
            return checkIntegrity(is, size, hash, new byte[8096], i -> {});
        }
    }

    public boolean checkIntegrity(InputStream is, @Nullable Long size, @Nullable String hash, byte[] buffer, IOConsumer<Integer> readBytesConsumer) throws IOException {
        int n = 0;
        int totalReadBytes = 0;
        MessageDigest digest = getHashFunction();
        while (n != -1) {
            n = is.read(buffer);
            readBytesConsumer.accept(n);
            if (n > 0) {
                digest.update(buffer, 0, n);
                totalReadBytes += n;
            }
        }

        return (size == null || size == totalReadBytes) && (hash == null || hash.equalsIgnoreCase(toHashString(digest.digest())));
    }

    public String hash(byte[] bytes) {
        return toHashString(hashBytes(bytes));
    }

    public byte[] hashBytes(byte[] bytes) {
        MessageDigest hashFunction = getHashFunction();
        hashFunction.digest(bytes);
        return hashFunction.digest(bytes);
    }

    public String toHashString(byte[] bytes) {
        StringBuilder hashBuilder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hashBuilder.append(String.format("%02x", b));
        }

        return hashBuilder.toString();
    }

    @SneakyThrows
    public MessageDigest getHashFunction() {
        return MessageDigest.getInstance("SHA-1");
    }

}

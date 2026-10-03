package io.github.headlesshq.headlessmc.net.hash;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcNoSuchAlgorithmException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Default implementation of {@link HashService}.
 */
@ApplicationScoped
public class HashServiceImpl implements HashService {
    private final Instance<HashAlgorithmProvider> providers;

    @Inject
    public HashServiceImpl(@Any Instance<HashAlgorithmProvider> providers) {
        this.providers = providers;
    }

    @Override
    public MessageDigest getAlgorithm(String name) throws HeadlessMcNoSuchAlgorithmException {
        return providers.stream()
            .map(provider -> provider.getAlgorithm(name))
            .flatMap(Optional::stream)
            .findFirst()
            .orElseThrow(() -> new HeadlessMcNoSuchAlgorithmException("Failed to find hash algorithm " + name));
    }

    @Override
    public Verifier verifier() {
        return new VerifierImpl(this);
    }

    @Override
    public Map<MessageDigest, String> getAlgorithms(Map<String, String> digests) throws HeadlessMcNoSuchAlgorithmException {
        boolean found = digests.isEmpty();
        Map<MessageDigest, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : digests.entrySet()) {
            try {
                result.put(getAlgorithm(entry.getKey()), entry.getValue());
                found = true;
            } catch (HeadlessMcNoSuchAlgorithmException ignored) {

            }
        }

        if (!found) {
            throw new HeadlessMcNoSuchAlgorithmException("Failed to find algorithms: " + digests);
        }

        return result;
    }

    @Override
    public List<HashAlgorithmProvider> getProviders() {
        return providers.stream().toList();
    }

    @Override
    public String toHexString(byte[] bytes) {
        return HexUtil.toHex(bytes);
    }

    @Override
    public HashResult hashFiles(Collection<Path> files, String algorithm) {
        long size = 0L;
        MessageDigest digest = getAlgorithm(algorithm);
        for (Path file : files) {
            try (
                InputStream inputStream = Files.newInputStream(file);
                DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)
            ) {
                long transferred = digestInputStream.transferTo(EmptyOutputStream.INSTANCE);
                size = Math.addExact(size, transferred);
            } catch (ArithmeticException | IOException e) {
                throw new HeadlessMcIOException("Failed to hash " + file, e);
            }
        }

        return new HashResult(toHexString(digest.digest()), size);
    }

}

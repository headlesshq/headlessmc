package io.github.headlesshq.headlessmc.net.hash;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcNoSuchAlgorithmException;
import io.github.headlesshq.headlessmc.exceptions.VerificationException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
final class VerifierImpl implements Verifier {
    private final HashService service;

    private final Map<String, String> algorithms = new HashMap<>();
    private final Map<MessageDigest, String> digests = new HashMap<>();
    private @Nullable Long size;

    @Override
    public Verifier hash(@Nullable String algorithm, @Nullable String hash) {
        if (algorithm != null && hash != null) {
            algorithms.put(algorithm, hash);
        }

        return this;
    }

    @Override
    public Verifier hash(@Nullable MessageDigest digest, @Nullable String hash) {
        if (digest != null && hash != null) {
            digests.put(digest, hash);
        }

        return this;
    }

    @Override
    public Verifier sha1(String digest) {
        return hash(HashService.SHA1, digest);
    }

    @Override
    public Verifier sha256(String digest) {
        return hash(HashService.SHA256, digest);
    }

    @Override
    public Verifier size(@Nullable Long size) {
        this.size = size;
        return this;
    }

    @Override
    public Verifier algorithms(Map<String, @Nullable String> algorithms) {
        algorithms.forEach(this::hash);
        return this;
    }

    @Override
    public Verifier digests(Map<MessageDigest, @Nullable String> algorithms) {
        algorithms.forEach(this::hash);
        return this;
    }

    @Override
    public void verify(InputStream inputStream)
        throws VerificationException, HeadlessMcIOException, HeadlessMcNoSuchAlgorithmException {
        Optional<InputStreamVerifier> verifier = buildObserver();
        InputStream stream = verifier
            .map(observer -> (InputStream) new ObservableInputStream(List.of(observer), inputStream))
            .orElse(inputStream);

        try {
            stream.transferTo(EmptyOutputStream.INSTANCE);
            if (verifier.isPresent() && !verifier.get().hasVerified()) {
                verifier.get().verify();
            }
        } catch (IOException e) {
            throw new HeadlessMcIOException(e);
        }
    }

    @Override
    public void verify(Path file)
        throws VerificationException, HeadlessMcIOException, HeadlessMcNoSuchAlgorithmException {
        try {
            long size = Files.size(file);
            Long expectedSize = this.size;
            if (expectedSize != null && expectedSize != size) {
                throw new VerificationException("Failed to verify file %s expected size %d but got %d".formatted(
                    file,
                    expectedSize,
                    size
                ));
            }

            try (InputStream inputStream = Files.newInputStream(file)) {
                verify(inputStream);
            }
        } catch (IOException e) {
            throw new HeadlessMcIOException(e);
        }
    }

    @Override
    public Optional<InputStreamVerifier> buildObserver() throws HeadlessMcNoSuchAlgorithmException {
        Map<MessageDigest, String> digests = getAllDigests();
        if (digests.isEmpty() && size == null) {
            return Optional.empty();
        }

        return Optional.of(new InputStreamVerifier(digests, size));
    }

    private Map<MessageDigest, String> getAllDigests() throws HeadlessMcNoSuchAlgorithmException {
        Map<MessageDigest, String> allDigests = new HashMap<>(digests);
        try {
            allDigests.putAll(service.getAlgorithms(algorithms));
        } catch (HeadlessMcNoSuchAlgorithmException e) {
            if (allDigests.isEmpty()) {
                throw e;
            } // else is okay, we have found some other algorithms
        }

        return allDigests;
    }

}

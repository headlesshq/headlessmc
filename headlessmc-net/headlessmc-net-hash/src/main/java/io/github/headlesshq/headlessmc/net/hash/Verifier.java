package io.github.headlesshq.headlessmc.net.hash;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcNoSuchAlgorithmException;
import io.github.headlesshq.headlessmc.exceptions.VerificationException;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;

/**
 * A Builder like object to build the hash/size verification of
 * {@link InputStream} or a {@link Path}.
 *
 * @see HashService
 * @see #verify(Path)
 * @see #verify(InputStream)
 */
public interface Verifier {
    Verifier hash(@Nullable String hashName, @Nullable String hash);

    Verifier hash(@Nullable MessageDigest digest, @Nullable String hash);

    Verifier sha1(String digest);

    Verifier sha256(String digest);

    Verifier size(@Nullable Long size);

    Verifier algorithms(Map<String, @Nullable String> algorithms);

    Verifier digests(Map<MessageDigest, @Nullable String> algorithms);

    void verify(InputStream inputStream)
        throws VerificationException, HeadlessMcIOException, HeadlessMcNoSuchAlgorithmException;

    void verify(Path file)
        throws VerificationException, HeadlessMcIOException, HeadlessMcNoSuchAlgorithmException;

    Optional<InputStreamVerifier> buildObserver() throws HeadlessMcNoSuchAlgorithmException;

}

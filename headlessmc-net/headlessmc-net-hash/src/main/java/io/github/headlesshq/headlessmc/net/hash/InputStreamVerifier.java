package io.github.headlesshq.headlessmc.net.hash;

import io.github.headlesshq.headlessmc.exceptions.VerificationException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;

import java.security.MessageDigest;
import java.util.Map;

@RequiredArgsConstructor
@Accessors(fluent = true)
public final class InputStreamVerifier implements ObservableInputStream.Observer {
    private final Map<MessageDigest, String> digests;
    private final @Nullable Long expectedSize;
    @Getter
    private volatile boolean hasVerified = false;

    private long bytesRead;

    @Override
    public void byteRead(byte b) throws VerificationException {
        digests.keySet().forEach(digest -> digest.update(b));
        bytesRead += 1;
        checkSizeTooLarge();
    }

    @Override
    public void bytesRead(byte[] b, int off, int len) throws VerificationException {
        digests.keySet().forEach(digest -> digest.update(b, off, len));
        bytesRead += len;
        checkSizeTooLarge();
    }

    @Override
    public void eof() throws VerificationException {
        verify();
    }

    public synchronized void verify() throws VerificationException {
        if (hasVerified) {
            return;
        }

        hasVerified = true;
        if (expectedSize != null && expectedSize != bytesRead) {
            throw new VerificationException("Wrong size, expected %d bytes but got %d bytes".formatted(
                expectedSize,
                bytesRead
            ));
        }

        for (Map.Entry<MessageDigest, String> entry : digests.entrySet()) {
            MessageDigest md = entry.getKey();
            String expectedHex = entry.getValue();
            String actualHex = HexUtil.toHex(md.digest());
            if (!actualHex.equalsIgnoreCase(expectedHex)) {
                throw new VerificationException(
                    "Digest mismatch for " + md.getAlgorithm() +
                        ": expected " + expectedHex + ", got " + actualHex
                );
            }
        }
    }

    private void checkSizeTooLarge() throws VerificationException {
        if (expectedSize != null && bytesRead > expectedSize) {
            throw new VerificationException("Wrong size, expected %d bytes but found more".formatted(expectedSize));
        }
    }

}

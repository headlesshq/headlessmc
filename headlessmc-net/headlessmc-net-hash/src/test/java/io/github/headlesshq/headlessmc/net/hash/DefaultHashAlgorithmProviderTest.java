package io.github.headlesshq.headlessmc.net.hash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultHashAlgorithmProviderTest {
    private final DefaultHashAlgorithmProvider provider = new DefaultHashAlgorithmProvider();

    @ParameterizedTest
    @CsvSource({
        "md5, MD5",
        "MD-5, MD5",
        "sha1, SHA-1",
        "SHA_1, SHA-1",
        "sha256, SHA-256",
        "SHA-256, SHA-256",
        "sha224, SHA-224",
        "sha384, SHA-384",
        "SHA_512, SHA-512",
        "sha3_256, SHA3-256",
        "SHA-512, SHA-512",
    })
    public void resolvesLenientNames(String input, String expected) {
        assertEquals(expected, provider.getAlgorithm(input).orElseThrow().getAlgorithm());
    }

    @Test
    public void returnsEmptyForUnknownAlgorithm() {
        assertTrue(provider.getAlgorithm("no-such-algorithm").isEmpty());
    }

}

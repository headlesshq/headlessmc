package io.github.headlesshq.headlessmc.net.hash;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcNoSuchAlgorithmException;
import io.quarkus.test.component.QuarkusComponentTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusComponentTest({HashServiceImpl.class, DefaultHashAlgorithmProvider.class})
public class HashServiceTest {
    @Inject
    HashService hashService;

    @Test
    public void findsAlgorithmsLeniently() {
        assertEquals("SHA-256", hashService.getAlgorithm("sha256").getAlgorithm());
        assertEquals("SHA-1", hashService.getAlgorithm("SHA_1").getAlgorithm());
        assertEquals("MD5", hashService.getAlgorithm("md5").getAlgorithm());
    }

    @Test
    public void throwsOnUnknownAlgorithm() {
        assertThrows(HeadlessMcNoSuchAlgorithmException.class,
            () -> hashService.getAlgorithm("no-such-algorithm"));
    }

    @Test
    public void resolvesAlgorithmMapIgnoringUnknowns() {
        Map<MessageDigest, String> result = hashService.getAlgorithms(Map.of(
            "SHA-256", "abc",
            "no-such-algorithm", "def"
        ));
        assertEquals(1, result.size());
        assertEquals("abc", result.values().iterator().next());
    }

    @Test
    public void emptyAlgorithmMapResolvesToEmptyMap() {
        assertTrue(hashService.getAlgorithms(Map.of()).isEmpty());
    }

    @Test
    public void throwsIfNoAlgorithmCanBeResolved() {
        assertThrows(HeadlessMcNoSuchAlgorithmException.class,
            () -> hashService.getAlgorithms(Map.of("no-such-algorithm", "abc")));
    }

    @Test
    public void hasProviders() {
        assertFalse(hashService.getProviders().isEmpty());
    }

    @Test
    public void convertsBytesToHex() {
        assertEquals("00ff10", hashService.toHexString(new byte[]{0x00, (byte) 0xff, 0x10}));
    }

    @Test
    public void hashesMultipleFiles(@TempDir Path dir) throws Exception {
        byte[] first = "hello ".getBytes(StandardCharsets.UTF_8);
        byte[] second = "world".getBytes(StandardCharsets.UTF_8);
        Path a = dir.resolve("a.bin");
        Path b = dir.resolve("b.bin");
        Files.write(a, first);
        Files.write(b, second);

        HashService.HashResult result = hashService.hashFiles(List.of(a, b), "SHA-256");

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(first);
        digest.update(second);
        assertEquals(HexFormat.of().formatHex(digest.digest()), result.hash());
        assertEquals(first.length + second.length, result.size());
    }

    @Test
    public void hashFilesThrowsOnMissingFile(@TempDir Path dir) {
        Path missing = dir.resolve("missing.bin");
        assertThrows(HeadlessMcIOException.class,
            () -> hashService.hashFiles(List.of(missing), "SHA-256"));
    }

}

package io.github.headlesshq.headlessmc.net.hash;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcNoSuchAlgorithmException;
import io.github.headlesshq.headlessmc.exceptions.VerificationException;
import io.quarkus.test.component.QuarkusComponentTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusComponentTest({HashServiceImpl.class, DefaultHashAlgorithmProvider.class})
public class VerifierTest {
    private static final byte[] DATA = "hello world".getBytes(StandardCharsets.UTF_8);

    @Inject
    HashService hashService;

    @Test
    public void verifiesStreamWithCorrectHashesAndSize() throws Exception {
        hashService.verifier()
            .sha1(hex("SHA-1"))
            .sha256(hex("SHA-256"))
            .size((long) DATA.length)
            .verify(stream());
    }

    @Test
    public void throwsOnWrongHash() {
        Verifier verifier = hashService.verifier().sha256("00000000");
        assertThrows(VerificationException.class, () -> verifier.verify(stream()));
    }

    @Test
    public void throwsOnSizeMismatch() throws Exception {
        Verifier tooLarge = hashService.verifier().size((long) DATA.length - 1);
        assertThrows(VerificationException.class, () -> tooLarge.verify(stream()));

        Verifier tooSmall = hashService.verifier().size((long) DATA.length + 1);
        assertThrows(VerificationException.class, () -> tooSmall.verify(stream()));
    }

    @Test
    public void verifiesFile(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("data.bin");
        Files.write(file, DATA);
        hashService.verifier()
            .sha256(hex("SHA-256"))
            .size((long) DATA.length)
            .verify(file);
    }

    @Test
    public void throwsOnWrongFileSize(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("data.bin");
        Files.write(file, DATA);
        Verifier verifier = hashService.verifier().size(1L);
        VerificationException e = assertThrows(VerificationException.class, () -> verifier.verify(file));
        assertTrue(e.getMessage().contains("expected size"));
    }

    @Test
    public void buildsNoObserverWithoutHashesOrSize() throws Exception {
        assertTrue(hashService.verifier().buildObserver().isEmpty());
    }

    @Test
    public void ignoresNullHashesAndAlgorithms() throws Exception {
        Verifier verifier = hashService.verifier()
            .hash((String) null, null)
            .hash("SHA-256", null)
            .hash((MessageDigest) null, "abc");
        assertTrue(verifier.buildObserver().isEmpty());
    }

    @Test
    public void acceptsLenientAlgorithmNames() throws Exception {
        hashService.verifier()
            .hash("sha256", hex("SHA-256"))
            .verify(stream());
    }

    @Test
    public void throwsOnUnknownAlgorithm() {
        Verifier verifier = hashService.verifier().hash("no-such-algorithm", "abc");
        assertThrows(HeadlessMcNoSuchAlgorithmException.class, () -> verifier.verify(stream()));
    }

    @Test
    public void ignoresUnknownAlgorithmIfOthersAreKnown() throws Exception {
        hashService.verifier()
            .hash("no-such-algorithm", "abc")
            .sha256(hex("SHA-256"))
            .verify(stream());
    }

    @Test
    public void verifiesWithExplicitDigestsAndAlgorithmMaps() throws Exception {
        hashService.verifier()
            .digests(Map.of(MessageDigest.getInstance("SHA-1"), hex("SHA-1")))
            .algorithms(Map.of("SHA-256", hex("SHA-256")))
            .verify(stream());
    }

    private static InputStream stream() {
        return new ByteArrayInputStream(DATA);
    }

    private static String hex(String algorithm) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(DATA));
    }

}

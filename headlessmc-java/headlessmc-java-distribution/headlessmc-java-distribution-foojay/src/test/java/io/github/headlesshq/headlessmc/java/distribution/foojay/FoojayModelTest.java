package io.github.headlesshq.headlessmc.java.distribution.foojay;

import io.github.headlesshq.headlessmc.distribution.JavaDistribution;
import io.github.headlesshq.headlessmc.distribution.JavaRuntime;
import io.github.headlesshq.headlessmc.net.rest.ApiException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FoojayModelTest {
    @Test
    void distributionIsConvertedToJavaDistribution() {
        JavaDistribution distribution = new Distribution("Temurin", "temurin").toJavaDistribution("foojay.io");

        assertEquals(new JavaDistribution("foojay.io", "Temurin", "temurin"), distribution);
    }

    @Test
    void packageStripsTheArchiveTypeFromTheName() {
        Pkg pkg = new Pkg("id", "OpenJDK25U-jre_x64_linux.tar.gz", 42L, "tar.gz");

        JavaRuntime runtime = pkg.toRuntime("foojay.io", 25, "temurin");

        assertEquals("OpenJDK25U-jre_x64_linux", runtime.name());
        assertEquals("id", runtime.id());
        assertEquals(25, runtime.version());
        assertEquals("temurin", runtime.distribution());
        assertEquals(new JavaRuntime.MetaData(42L, "tar.gz"), runtime.metaData());
    }

    @Test
    void packageKeepsTheNameWithoutMatchingArchiveType() {
        assertEquals("archive.zip", new Pkg("id", "archive.zip", null, null).toRuntime("p", 21, "d").name());
        assertEquals("archive.zip", new Pkg("id", "archive.zip", null, "tar.gz").toRuntime("p", 21, "d").name());
    }

    @Test
    void hashAlgorithmNamesAreMappedToMessageDigestNames() {
        assertEquals("MD5", info("md5").getHashAlgorithmName());
        assertEquals("SHA-1", info("sha1").getHashAlgorithmName());
        assertEquals("SHA-256", info("sha256").getHashAlgorithmName());
        assertEquals("SHA-224", info("sha224").getHashAlgorithmName());
        assertEquals("SHA-384", info("sha384").getHashAlgorithmName());
        assertEquals("SHA-512", info("sha512").getHashAlgorithmName());
        assertEquals("SHA3-256", info("sha3_256").getHashAlgorithmName());
    }

    @Test
    void unknownOrMissingHashAlgorithmIsNull() {
        assertNull(info(null).getHashAlgorithmName());
        assertNull(info("not-an-algorithm").getHashAlgorithmName());
    }

    @Test
    void resultResolvesItsValue() {
        assertEquals(List.of("a"), new Result<>(List.of("a")).resolve(null));
    }

    @Test
    void emptyResultThrows() {
        ApiException e = assertThrows(ApiException.class, () -> new Result<>(null).resolve(null));
        assertTrue(e.getMessage().contains("No result available"));
    }

    private PkgInfo info(String checksumType) {
        return new PkgInfo("https://example.com/jdk.tar.gz", "abc", checksumType, "jdk.tar.gz");
    }

}

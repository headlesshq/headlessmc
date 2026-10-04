package io.github.headlesshq.headlessmc.java.distribution.foojay;

import eu.hansolo.jdktools.HashAlgorithm;
import org.jspecify.annotations.Nullable;

import java.security.MessageDigest;

/**
 * <pre>
 * {@code
 * {
 *     "filename":"OpenJDK25U-jre_x64_linux_hotspot_25_36.tar.gz",
 *     "direct_download_uri":"https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25%2B36/OpenJDK25U-jre_x64_linux_hotspot_25_36.tar.gz",
 *     "download_site_uri":"",
 *     "signature_uri":"https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25%2B36/OpenJDK25U-jre_x64_linux_hotspot_25_36.tar.gz.sig",
 *     "checksum_uri":"https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25%2B36/OpenJDK25U-jre_x64_linux_hotspot_25_36.tar.gz.sha256.txt",
 *     "checksum":"5e3de13a1487ecc90f8b0cddc83a6cd4e053b4cd48ddcfe5d1f19178e6089fba",
 *     "checksum_type":"sha256"
 *   }
 * }
 * </pre>
 *
 * @param direct_download_uri
 * @param checksum
 * @param checksum_type
 * @param size
 * @param archive_type
 * @param operating_system
 */
record PkgInfo(
    @Nullable String direct_download_uri,
    @Nullable String checksum,
    @Nullable String checksum_type,
    @Nullable String filename
) {
    /**
     * Converts {@link #checksum_type} to the name for a {@link MessageDigest}
     * in the format as specified by
     * <a href=https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html>
     * https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html
     * </a>.
     * Or returns {@code null} if {@link #checksum_type} is {@code null} or no
     * fitting {@link HashAlgorithm} could be found.
     *
     * @return {@link #checksum_type} as {@link MessageDigest} name or {@code null}.
     */
    public @Nullable String getHashAlgorithmName() {
        HashAlgorithm algorithm = getAlgorithm();
        if (algorithm == null) {
            return null;
        }

        switch (algorithm) {
            case MD5 -> {
                return "MD5";
            }
            case SHA1 -> {
                return "SHA-1";
            }
            case SHA256 -> {
                return "SHA-256";
            }
            case SHA224 -> {
                return "SHA-224";
            }
            case SHA384 -> {
                return "SHA-384";
            }
            case SHA512 -> {
                return "SHA-512";
            }
            case SHA3_256 -> {
                return "SHA3-256";
            }
            default -> {
                return null;
            }
        }
    }

    private @Nullable HashAlgorithm getAlgorithm() {
        String algorithmType = checksum_type();
        if (algorithmType != null) {
            return HashAlgorithm.fromText(algorithmType);
        }

        return null;
    }

}

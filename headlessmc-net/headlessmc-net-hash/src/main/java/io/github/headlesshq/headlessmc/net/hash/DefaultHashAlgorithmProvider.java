package io.github.headlesshq.headlessmc.net.hash;

import jakarta.enterprise.context.ApplicationScoped;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Optional;

/**
 * Default implementation of {@link HashAlgorithmProvider}.
 * Does lenient parsing of the hash name for some algorithm names.
 * E.g. will also accept {@code sha256} as {@code SHA-256}.
 */
@ApplicationScoped
public class DefaultHashAlgorithmProvider implements HashAlgorithmProvider {
    @Override
    public Optional<MessageDigest> getAlgorithm(String name) {
        try {
            return Optional.of(getAlgorithmLenient(name));
        } catch (NoSuchAlgorithmException e) {
            return Optional.empty();
        }
    }

    private MessageDigest getAlgorithmLenient(String hashName) throws NoSuchAlgorithmException {
        String name = hashName.toUpperCase(Locale.ENGLISH);
        switch(name) {
            case "MD5", "MD-5", "MD_5" -> { return MessageDigest.getInstance("MD5"); }
            case "SHA1", "SHA-1", "SHA_1" -> { return MessageDigest.getInstance("SHA-1"); }
            case "SHA256", "SHA_256", "SHA-256" -> { return MessageDigest.getInstance("SHA-256"); }
            case "SHA224", "SHA_224", "SHA-224"-> { return MessageDigest.getInstance("SHA-224"); }
            case "SHA384", "SHA_384", "SHA-384" -> { return MessageDigest.getInstance("SHA-384"); }
            case "SHA512", "SHA_512", "SHA-512" -> { return MessageDigest.getInstance("SHA-512"); }
            case "SHA3_256", "SHA-3-256", "SHA_3_256" -> { return MessageDigest.getInstance("SHA3-256"); }
            default -> { return MessageDigest.getInstance(name); }
        }
    }

}

package io.github.headlesshq.headlessmc.net.hash;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcNoSuchAlgorithmException;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface HashService {
    String SHA256 = "SHA-256";
    String SHA1 = "SHA-1";

    MessageDigest getAlgorithm(String name) throws HeadlessMcNoSuchAlgorithmException;

    Verifier verifier();

    Map<MessageDigest, String> getAlgorithms(Map<String, String> algorithmNames) throws HeadlessMcNoSuchAlgorithmException;

    List<HashAlgorithmProvider> getProviders();

    String toHexString(byte[] bytes);

    HashResult hashFiles(Collection<Path> files, String algorithm);

    record HashResult(String hash, long size) {}

}

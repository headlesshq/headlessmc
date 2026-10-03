package io.github.headlesshq.headlessmc.net;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
@Accessors(fluent = true, prefix = "")
public abstract class AbstractDownloadBuilder implements DownloadBuilder {
    protected final Map<MessageDigest, @Nullable String> resolvedHashAlgorithms = new HashMap<>();
    protected final Map<String, String> hashAlgorithms = new HashMap<>();
    protected final URI uri;

    protected @Nullable StatusCodeHandler statusCodeHandler = StatusCodeHandler.error();
    protected @Nullable DownloadRetry retry = DownloadRetry.none();
    protected @Nullable String progressBar;
    protected @Nullable Long size;

    @Override
    public DownloadBuilder hash(@Nullable String algorithmName, @Nullable String hash) {
        if (algorithmName != null && hash != null) {
            hashAlgorithms.put(algorithmName, hash);
        }

        return this;
    }

    @Override
    public DownloadBuilder hash(MessageDigest algorithm, @Nullable String hash) {
        if (hash != null) {
            resolvedHashAlgorithms.put(algorithm, hash);
        }

        return this;
    }

    @Override
    public DownloadBuilder hashes(Map<String, @Nullable String> hashes) {
        hashes.forEach(this::hash);
        return this;
    }

    @Override
    public DownloadBuilder sha1(@Nullable String sha1) {
        if (sha1 != null) {
            hashAlgorithms.put("SHA-1", sha1);
        }

        return this;
    }

    @Override
    public DownloadBuilder sha256(@Nullable String sha256) {
        if (sha256 != null) {
            hashAlgorithms.put("SHA-256", sha256);
        }

        return this;
    }

}

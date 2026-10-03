package io.github.headlesshq.headlessmc.net;

import io.github.headlesshq.headlessmc.exceptions.ExConsumer;
import io.github.headlesshq.headlessmc.exceptions.ExFunction;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import jakarta.enterprise.util.TypeLiteral;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Because {@link DownloadContext} is {@link AutoCloseable},
 * this is a good way for {@link DownloadService} implementations
 * to support {@link DownloadService#download(URI)},
 * because an implementation otherwise may just call
 * {@link DownloadService#context()} and then
 * {@link DownloadContext#download(URI)},
 * which is problematic as the Service cannot know
 * when the context can be closed.
 * This {@link DownloadBuilder} calls {@link DownloadService#context()}
 * only on the executing methods (e.g. {@link #start(ExConsumer)}),
 * and closes it on completion.
 */
@RequiredArgsConstructor
public class DelegatingDownloadBuilder implements DownloadBuilder {
    private final List<UnaryOperator<DownloadBuilder>> configurations = new ArrayList<>();
    private final DownloadService downloadService;
    private final URI url;

    private DownloadBuilder configuration(UnaryOperator<DownloadBuilder> configuration) {
        configurations.add(configuration);
        return this;
    }

    @Override
    public DownloadBuilder statusCodeHandler(@Nullable StatusCodeHandler handler) {
        return configuration(builder -> builder.statusCodeHandler(handler));
    }

    @Override
    public DownloadBuilder progressBar(@Nullable String title) {
        return configuration(builder -> builder.progressBar(title));
    }

    @Override
    public DownloadBuilder size(@Nullable Long size) {
        return configuration(builder -> builder.size(size));
    }

    @Override
    public DownloadBuilder hash(@Nullable String algorithmName, @Nullable String hash) {
        return configuration(builder -> builder.hash(algorithmName, hash));
    }

    @Override
    public DownloadBuilder hash(MessageDigest algorithm, @Nullable String hash) {
        return configuration(builder -> builder.hash(algorithm, hash));
    }

    @Override
    public DownloadBuilder hashes(Map<String, @Nullable String> hashes) {
        return configuration(builder -> builder.hashes(hashes));
    }

    @Override
    public DownloadBuilder sha1(@Nullable String sha1) {
        return configuration(builder -> builder.sha1(sha1));
    }

    @Override
    public DownloadBuilder sha256(@Nullable String sha256) {
        return configuration(builder -> builder.sha256(sha256));
    }

    @Override
    public DownloadBuilder retry(DownloadRetry retry) {
        return configuration(builder -> builder.retry(retry));
    }

    @Override
    public DownloadBuilder retry(int retries, int initialIntervalMs, double factor, double jitter) {
        return configuration(builder -> builder.retry(retries, initialIntervalMs, factor, jitter));
    }

    private <T> T execute(Function<DownloadBuilder, T> function) {
        try (DownloadContext context = downloadService.context()) {
            DownloadBuilder builder = context.download(url);
            for (UnaryOperator<DownloadBuilder> configuration : configurations) {
                builder = configuration.apply(builder);
            }

            return function.apply(builder);
        }
    }

    @Override
    public void start(ExConsumer<Download> action) throws HeadlessMcException {
        execute(builder -> {
            builder.start(action);
            return 0;
        });
    }

    @Override
    public <T> T map(ExFunction<Download, T> action) throws HeadlessMcException {
        return execute(builder -> builder.map(action));
    }

    @Override
    public <T extends ReflectionRegistered> T json(Class<T> type) throws HeadlessMcException {
        return execute(builder -> builder.json(type));
    }

    @Override
    public <T extends ReflectionRegistered> T json(TypeLiteral<T> typeLiteral) throws HeadlessMcException {
        return execute(builder -> builder.json(typeLiteral));
    }

    @Override
    public <T> T jsonRegisteredForReflection(TypeLiteral<T> typeLiteral) throws HeadlessMcException {
        return execute(builder -> builder.jsonRegisteredForReflection(typeLiteral));
    }

    @Override
    public Path toFile(Path file) throws HeadlessMcException {
        return execute(builder -> builder.toFile(file));
    }

}

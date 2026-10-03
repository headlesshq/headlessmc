package io.github.headlesshq.headlessmc.net.java;

import io.github.headlesshq.headlessmc.exceptions.*;
import io.github.headlesshq.headlessmc.net.*;
import io.github.headlesshq.headlessmc.net.hash.HashService;
import io.github.headlesshq.headlessmc.net.hash.InputStreamVerifier;
import io.github.headlesshq.headlessmc.net.hash.ObservableInputStream;
import io.github.headlesshq.headlessmc.progressbar.ProgressBar;
import io.github.headlesshq.headlessmc.progressbar.ProgressBarServiceManager;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import jakarta.enterprise.util.TypeLiteral;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

@Slf4j
final class JavaDownloadBuilder extends AbstractDownloadBuilder {
    private final ProgressBarServiceManager progressBarService;
    private final JsonService jsonService;
    private final HashService hashService;
    private final HttpClient client;
    private final NetConfig config;

    public JavaDownloadBuilder(
        URI uri,
        ProgressBarServiceManager progressBarService, JsonService jsonService,
        HashService hashService,
        HttpClient client,
        NetConfig config
    ) {
        super(uri);
        this.progressBarService = progressBarService;
        this.jsonService = jsonService;
        this.hashService = hashService;
        this.client = client;
        this.config = config;
        if (config.retries() > 0) {
            retry(config.retries(), 1000, 2.0, 0.5);
        }
    }

    @Override
    public DownloadBuilder retry(int retries, int initialIntervalMs, double factor, double jitter) {
        return retry(new Resilience4JRetry(retries, initialIntervalMs, factor, jitter));
    }

    private DownloadRetry getRetry() {
        DownloadRetry retry = this.retry();
        return retry == null ? DownloadRetry.none() : retry;
    }

    @Override
    public void start(ExConsumer<Download> action) throws HeadlessMcException {
        map(download -> {
            action.accept(download);
            return 0;
        });
    }

    @Override
    public <T> T map(ExFunction<Download, T> action) throws HeadlessMcException {
        DownloadRetry retry = getRetry();
        try {
            return retry.run(() -> mapWithoutRetry(action));
        } catch (HeadlessMcException e) {
            throw e;
        } catch (InterruptedException | UncheckedInterruptedException e) {
            throw new UncheckedInterruptedException(e);
        } catch (/* okay-to-catch-marker */Exception e) {
            throw new RequestException(e);
        }
    }

    @Override
    public <T extends ReflectionRegistered> T json(Class<T> type) throws HeadlessMcException {
        return map(download -> {
            try (InputStream inputStream = download.getInputStream()) {
                return jsonService.parse(inputStream, type);
            }
        });
    }

    @Override
    public <T extends ReflectionRegistered> T json(TypeLiteral<T> typeLiteral) throws HeadlessMcException {
        return map(download -> {
            try (InputStream inputStream = download.getInputStream()) {
                return jsonService.parse(inputStream, typeLiteral);
            }
        });
    }

    @Override
    public <T> T jsonRegisteredForReflection(TypeLiteral<T> typeLiteral) throws HeadlessMcException {
        return map(download -> {
            try (InputStream inputStream = download.getInputStream()) {
                return jsonService.parseRegisteredForReflection(inputStream, typeLiteral);
            }
        });
    }

    @Override
    public Path toFile(Path file) throws HeadlessMcException {
        if (Files.exists(file) && Files.isDirectory(file)) {
            throw new FileException("File " + file + " exists and is directory");
        }

        start(download -> {
            try (InputStream inputStream = download.getInputStream()) {
                Path parent = file.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }

                try (OutputStream outputStream = Files.newOutputStream(file)) {
                    inputStream.transferTo(outputStream);
                } catch (IOException e) {
                    if (config.deleteFailedFiles()) {
                        try {
                            Files.deleteIfExists(file);
                        } catch (IOException deleteException) {
                            e.addSuppressed(deleteException);
                        }
                    }

                    throw e;
                }
            }
        });

        return file;
    }

    private <T> T mapWithoutRetry(ExFunction<Download, T> action) throws Exception {
        Optional<InputStreamVerifier> verifier = getVerifier();

        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(uri())
            .build();

        StatusCodeHandler statusCodeHandler = statusCodeHandler();
        if (statusCodeHandler == null) {
            statusCodeHandler = (_, __) -> {
            };
        }

        HttpResponse<InputStream> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            throw new RequestException(e);
        } catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        }

        InputStream in = response.body();
        InputStream finalIn = in;
        try {
            statusCodeHandler.accept(
                response.statusCode(),
                () -> new String(finalIn.readAllBytes(), StandardCharsets.UTF_8)
            );

            OptionalLong contentLength = response.headers().firstValueAsLong("Content-Length");
            Long potentialSize = contentLength.orElse(in.available());
            if (potentialSize <= 0) {
                potentialSize = null;
            }

            in = wrap(verifier.orElse(null), potentialSize, in);
            T result = action.apply(new DownloadImpl(Optional.ofNullable(potentialSize), response.statusCode(), in));
            if (verifier.isPresent() && !verifier.get().hasVerified()) {
                // See ModpackDownloader
                // before we call this, we might have to read the rest of the stream
                // TODO: I think a problem can be if the InputStream does not need to be read fully?
                // mod download --type modpack vanilla-perfected fabric-26.2
                // in this case the zip of the mrpack was not read fully?
                //  expected 6337780 bytes but got 6329152 bytes
                verifier.get().verify();
            }

            return result;
        } finally {
            in.close();
        }
    }

    private Optional<InputStreamVerifier> getVerifier() {
        return hashService.verifier()
            .algorithms(hashAlgorithms)
            .digests(resolvedHashAlgorithms)
            .size(size)
            .buildObserver();
    }

    private ObservableInputStream.Observer progressBar(String title, @Nullable Long expectedSize) {
        ProgressBar progressBar = progressBarService.displayProgressBar(new ProgressBar.Configuration(
            title,
            expectedSize == null ? ProgressBar.Configuration.NO_INITIAL_SIZE : expectedSize,
            ProgressBar.Configuration.Unit.MB
        ));

        return new ProgressBarInputStreamObserver(progressBar);
    }

    private InputStream wrap(
        ObservableInputStream.@Nullable Observer verifier,
        @Nullable Long expectedSize,
        InputStream inputStream
    ) {
        List<ObservableInputStream.Observer> observers = new ArrayList<>(2);
        String title = progressBar;
        if (progressBar != null) {
            observers.add(progressBar(title, expectedSize));
        }

        if (verifier != null) {
            observers.add(verifier);
        }

        return new ObservableInputStream(observers, inputStream);
    }

}

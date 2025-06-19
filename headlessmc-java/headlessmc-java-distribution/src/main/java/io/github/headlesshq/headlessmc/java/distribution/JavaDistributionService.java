package io.github.headlesshq.headlessmc.java.distribution;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class JavaDistributionService /* TODO: check how this works with CDI: implements JavaDownloader*/ {
    public static final String DEFAULT_DISTRIBUTION = "temurin";

    private final Map<String, JavaDistribution> distributions;

    @Inject
    public JavaDistributionService(@Any Instance<JavaDistribution> downloaders) {
        this(downloaders.stream().collect(Collectors.toList()));
    }

    public JavaDistributionService(List<JavaDistribution> downloaders) {
        this.distributions = new HashMap<>();
        for (JavaDistribution downloader : downloaders) {
            this.distributions.put(downloader.getDistribution().toLowerCase(Locale.ENGLISH), downloader);
        }
    }

    public Path download(Path javaVersionsDir, JavaDownloadRequest request) throws IOException {
        if (Files.exists(javaVersionsDir) && !Files.isDirectory(javaVersionsDir)) {
            throw new IOException(javaVersionsDir.toAbsolutePath() + " is not a directory");
        }

        if (!Files.exists(javaVersionsDir)) {
            Files.createDirectories(javaVersionsDir);
        }

        JavaDistribution downloader = distributions.get(request.getDistribution() == null
                ? DEFAULT_DISTRIBUTION
                : request.getDistribution().toLowerCase(Locale.ENGLISH));

        if (downloader == null) {
            throw new IOException("Failed to find downloader for distribution " + request.getDistribution() + ", available: " + distributions.keySet());
        }

        return downloader.download(javaVersionsDir, request);
    }

    @Unmodifiable
    public Collection<JavaDistribution> getDistributions() {
        return distributions.values();
    }

}

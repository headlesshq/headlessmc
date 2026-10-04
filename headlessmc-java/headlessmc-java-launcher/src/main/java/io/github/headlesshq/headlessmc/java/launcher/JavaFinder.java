package io.github.headlesshq.headlessmc.java.launcher;

import io.github.headlesshq.headlessmc.distribution.JavaDistribution;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProvider;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProviderService;
import io.github.headlesshq.headlessmc.distribution.JavaRuntime;
import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaConfig;
import io.github.headlesshq.headlessmc.java.JavaService;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class JavaFinder {
    private final JavaDistributionProviderService distributionService;
    private final JavaService javaService;
    private final Holder<JavaConfig> config;
    private final AppFiles appFiles;
    private final CPU cpu;
    private final OS os;

    public Java findJava(int version) {
        Optional<Java> result = javaService.getJava(version);
        if (result.isPresent()) {
            return result.get();
        }

        if (config.get().download()) {
            return download(version);
        }

        throw new JavaProcessException("Failed to find Java " + version);
    }

    private Java download(int version) {
        JavaDistributionProvider provider = distributionService.getDefaultProvider();
        JavaDistribution distribution = provider.getDistributionByName(JavaDistribution.DEFAULT);
        List<JavaRuntime> runtimes = provider.getRuntimes(distribution, version, os, cpu);
        if (runtimes.isEmpty()) {
            throw new JavaProcessException.InstallationException(
                "Failed to find Java version " + version + " with provider " + provider.getName()
            );
        }

        JavaRuntime runtime = runtimes.getFirst();
        Path path = appFiles.getJavaDir().resolve(runtime.name());
        provider.download(runtime, path);
        javaService.refresh();
        Optional<Java> result = javaService.getJava(version);
        if (result.isPresent()) {
            return result.get();
        }

        throw new JavaProcessException.InstallationException(
            "Failed to find java " + version + " even after installing " + path
        );
    }

}

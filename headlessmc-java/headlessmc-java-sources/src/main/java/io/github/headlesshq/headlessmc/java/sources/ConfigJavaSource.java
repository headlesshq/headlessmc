package io.github.headlesshq.headlessmc.java.sources;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaConfig;
import io.github.headlesshq.headlessmc.java.JavaSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;

import java.util.List;

@ApplicationScoped
@Named("java:source:config")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ConfigJavaSource implements JavaSource {
    private final JavaScannerService javaScannerService;
    private final Holder<JavaConfig> config;

    @Override
    public String getName() {
        return "config";
    }

    @Override
    public List<Java> getJavas() {
        return config.get().versions().orElse(List.of())
            .stream()
            .map(dir -> (dir.toString().endsWith("bin/java") || dir.toString().endsWith("bin/java.exe"))
                ? dir.getParent().getParent()
                : dir)
            .filter(dir -> dir != null && !dir.toString().isBlank())
            .map(dir -> javaScannerService.scanDir(this, dir)
                .orElseThrow(() -> new HeadlessMcIOException("Failed to find Java version specified in config: " + dir))
            ).toList();
    }

    @Override
    public int sort() {
        return JavaSource.SORT_CONFIG;
    }

}

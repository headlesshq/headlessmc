package io.github.headlesshq.headlessmc.java;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JavaServiceImpl implements JavaService {
    private final Instance<JavaSource> sources;
    private @Nullable List<Java> javaVersions;

    @Inject
    public JavaServiceImpl(@Any Instance<JavaSource> sources) {
        this.sources = sources;
    }

    @Override
    public Optional<Java> getJava(int version) {
        return getJavaVersions()
            .stream()
            .filter(java -> version == java.version())
            .findFirst();
    }

    @Override
    public List<Java> getJavaVersions() {
        List<Java> versions = this.javaVersions;
        if (versions == null) {
            versions = sources.stream()
                .map(JavaSource::getJavas)
                .flatMap(List::stream)
                .sorted()
                .toList();

            this.javaVersions = versions;
        }

        return versions;
    }

    @Override
    public List<JavaSource> getSources() {
        return sources.stream()
            .sorted()
            .toList();
    }

    @Override
    public void refresh() {
        javaVersions = null;
    }

}

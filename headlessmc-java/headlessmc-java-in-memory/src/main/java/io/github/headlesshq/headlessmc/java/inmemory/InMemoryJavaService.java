package io.github.headlesshq.headlessmc.java.inmemory;

import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaService;
import io.github.headlesshq.headlessmc.java.JavaSource;
import io.github.headlesshq.headlessmc.java.sources.CurrentJavaSource;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

/**
 * A {@link JavaService} that only knows the Java running this JVM.
 * Programs are run in-memory by the {@link InMemoryJavaLauncherService},
 * so every requested Java version is served by the current Java.
 * Other installations are not scanned, because that requires starting processes.
 */
@Slf4j
@ApplicationScoped
@IfBuildProfile(InMemoryJavaLauncherService.PROFILE)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class InMemoryJavaService implements JavaService {
    private final CurrentJavaSource currentJavaSource;

    @Override
    public Optional<Java> getJava(int version) {
        Optional<Java> current = getJavaVersions().stream().findFirst();
        current.filter(java -> java.version() != version).ifPresent(java -> log.warn(
            "Java {} requested, using the current Java {}, processes cannot be started in-memory",
            version, java.version()
        ));

        return current;
    }

    @Override
    public List<Java> getJavaVersions() {
        return currentJavaSource.getJavas();
    }

    @Override
    public List<JavaSource> getSources() {
        return List.of(currentJavaSource);
    }

    @Override
    public void refresh() {
        // nothing to refresh, the current Java does not change
    }

}

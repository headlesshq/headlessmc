package io.github.headlesshq.headlessmc.java.inmemory;

import io.github.headlesshq.headlessmc.java.JavaService;
import io.github.headlesshq.headlessmc.java.launcher.JavaLauncherService;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessBuilder;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

/**
 * A {@link JavaLauncherService} that runs programs inside this JVM,
 * see {@link InMemoryProcessBuilder}.
 */
@ApplicationScoped
@IfBuildProfile(InMemoryJavaLauncherService.PROFILE)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class InMemoryJavaLauncherService implements JavaLauncherService {
    /**
     * The Quarkus build profile that replaces the process based services with the in-memory ones.
     * A build profile, because the Quarkus Gradle plugin only passes {@code quarkus.*} properties to the build.
     */
    public static final String PROFILE = "in-memory";

    private final JavaService javaService;

    @Override
    public JavaProcessBuilder buildProcess() {
        return new InMemoryProcessBuilder(javaService);
    }

    @Override
    public String getName() {
        return "in-memory";
    }

}

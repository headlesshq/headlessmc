package io.github.headlesshq.headlessmc.patcher.probe.strategy;

import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.java.launcher.JavaLauncherService;
import io.github.headlesshq.headlessmc.patcher.PatchContext;
import io.github.headlesshq.headlessmc.patcher.PatchException;
import io.github.headlesshq.headlessmc.patcher.asm.SuperClassResolver;
import io.github.headlesshq.headlessmc.patcher.asm.SuperClassStrategy;
import io.quarkus.runtime.annotations.RegisterResources;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Dependent
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@RegisterResources(globs = SuperClassProbeStrategy.RESOURCE_NAME)
public class SuperClassProbeStrategy implements SuperClassStrategy {
    // is injected by build.gradle from the headlessmc-patcher-probe project
    static final String RESOURCE_NAME = "patcher/probe/probe.jar";

    private final JavaLauncherService javaLauncherService;
    private final FileService fileService;

    @Override
    public SuperClassResolver apply(PatchContext context) {
        try {
            Path probeJar = Files.createTempFile("probe", ".jar");
            try (
                InputStream inputStream = Objects.requireNonNull(
                    getClass().getClassLoader().getResourceAsStream(RESOURCE_NAME),
                    "Failed to find resource " + RESOURCE_NAME
                );
                OutputStream outputStream = Files.newOutputStream(probeJar)
            ) {
                inputStream.transferTo(outputStream);
            }

            return new ProbeResolver(javaLauncherService, context.getJavaVersion(), probeJar);
        } catch (IOException e) {
            throw new PatchException("Failed to install probe.jar");
        }
    }

    @Override
    public boolean isApplicable() {
        return true;
    }

    @Override
    public int sort() {
        return SORT_PROBE;
    }

}

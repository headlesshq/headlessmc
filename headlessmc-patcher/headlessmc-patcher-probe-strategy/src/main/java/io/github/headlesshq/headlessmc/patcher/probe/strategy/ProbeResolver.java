package io.github.headlesshq.headlessmc.patcher.probe.strategy;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.java.launcher.JavaLauncherService;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcess;
import io.github.headlesshq.headlessmc.patcher.PatchException;
import io.github.headlesshq.headlessmc.patcher.asm.SuperClassResolver;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
final class ProbeResolver implements SuperClassResolver {
    private final AtomicBoolean closed = new AtomicBoolean();
    private final JavaLauncherService javaLauncherService;
    private final int javaVersion;
    private final Path probeJar;

    private @Nullable ProbeProcess probeProcess;

    @Override
    public synchronized String getCommonSuperClass(String type1, String type2) {
        if (closed.get()) {
            throw new PatchException("ProbeResolver used after closing");
        }

        ProbeProcess process = getOrStartProcess();
        try {
            return process.getCommonSuperClass(type1, type2);
        } catch (HeadlessMcIOException e) {
            try {
                process.close();
            } finally {
                probeProcess = null;
            }

            throw new PatchException("Failed to get common super class of " + type1 + ", " + type2, e);
        }
    }

    @Override
    public synchronized void close() throws HeadlessMcException {
        closed.set(true);
        ProbeProcess process = probeProcess;
        if (process != null) {
            process.close();
        }
    }

    private synchronized ProbeProcess getOrStartProcess() {
        ProbeProcess result = probeProcess;
        if (result != null) {
            return result;
        }

        JavaProcess process = javaLauncherService.buildProcess()
            .id("probe-super-class")
            .version(javaVersion)
            .jar(probeJar)
            .pipeIO(true)
            .start();

        probeProcess = ProbeProcess.of(process);
        return probeProcess;
    }

}

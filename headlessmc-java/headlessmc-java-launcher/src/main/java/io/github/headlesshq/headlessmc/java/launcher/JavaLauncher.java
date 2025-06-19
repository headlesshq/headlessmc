package io.github.headlesshq.headlessmc.java.launcher;

import io.github.headlesshq.headlessmc.java.service.JavaService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Stream;

import static java.util.Collections.synchronizedMap;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class JavaLauncher {
    private final Map<JavaProcess, @Nullable Object> processes = synchronizedMap(new WeakHashMap<>());
    private final JavaService javaService;

    public JavaProcess.JavaProcessBuilder createProcess() {
        return new JavaProcess.JavaProcessBuilder(this, javaService);
    }

    public Stream<JavaProcess> getProcesses() {
        return processes.keySet().stream();
    }

    final void addProcess(JavaProcess process) {
        processes.put(process, null);
    }

}

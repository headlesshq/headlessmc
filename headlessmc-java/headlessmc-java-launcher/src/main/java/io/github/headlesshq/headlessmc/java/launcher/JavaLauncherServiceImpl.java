package io.github.headlesshq.headlessmc.java.launcher;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.files.FileSystemProvider;
import io.github.headlesshq.headlessmc.java.JavaConfig;
import io.github.headlesshq.headlessmc.java.JavaService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class JavaLauncherServiceImpl implements JavaLauncherService {
    final JavaService javaService;
    final JavaFinder javaFinder;
    final FileSystemProvider fs;
    final Holder<JavaConfig> config;

    @Override
    public JavaProcessBuilder buildProcess() {
        return new ProcessBuilderImpl(this);
    }

    @Override
    public String getName() {
        return "default";
    }

}

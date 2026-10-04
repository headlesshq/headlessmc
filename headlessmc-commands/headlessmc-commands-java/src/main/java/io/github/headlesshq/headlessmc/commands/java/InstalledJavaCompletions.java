package io.github.headlesshq.headlessmc.commands.java;

import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaService;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;

import java.util.Iterator;

@Default
@Dependent
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Named("headlessmc:completions:java:installed")
public class InstalledJavaCompletions implements Iterable<String> {
    private final JavaService javaService;

    @Override
    public Iterator<String> iterator() {
        return javaService.getJavaVersions().stream()
            .map(Java::name)
            .iterator();
    }

}

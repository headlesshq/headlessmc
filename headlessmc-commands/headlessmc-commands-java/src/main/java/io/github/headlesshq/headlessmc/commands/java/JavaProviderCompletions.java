package io.github.headlesshq.headlessmc.commands.java;

import io.github.headlesshq.headlessmc.distribution.JavaDistributionProvider;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProviderService;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;

import java.util.Iterator;

@Default
@Dependent
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Named("headlessmc:completions:java:providers")
public class JavaProviderCompletions implements Iterable<String> {
    private final JavaDistributionProviderService service;

    @Override
    public Iterator<String> iterator() {
        return service.getProviders().stream()
            .map(JavaDistributionProvider::getName)
            .iterator();
    }

}

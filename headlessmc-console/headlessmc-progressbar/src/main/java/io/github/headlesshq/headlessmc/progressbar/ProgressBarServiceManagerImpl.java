package io.github.headlesshq.headlessmc.progressbar;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@ApplicationScoped
public class ProgressBarServiceManagerImpl implements ProgressBarServiceManager {
    private final Instance<ProgressbarService> services;
    private final ProgressbarService dummy;

    public ProgressBarServiceManagerImpl(Instance<ProgressbarService> services, @Dummy ProgressbarService dummy) {
        this.services = services;
        this.dummy = dummy;
    }

    @Override
    public ProgressBar displayProgressBar(ProgressBar.Configuration configuration) {
        return services.stream()
            .filter(service -> !service.isDummy())
            .map(service -> {
                try {
                    return Optional.of(service.displayProgressBar(configuration));
                } catch (HeadlessMcIOException e) {
                    log.error("Failed to provide progressbar with service {}", service.getName(), e);
                    return Optional.<ProgressBar>empty();
                }
            }).flatMap(Optional::stream)
            .findFirst()
            .orElseGet(() -> dummy.displayProgressBar(configuration));
    }

}

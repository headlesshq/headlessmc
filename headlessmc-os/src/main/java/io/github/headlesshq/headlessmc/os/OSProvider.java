package io.github.headlesshq.headlessmc.os;

import io.github.headlesshq.headlessmc.api.settings.Config;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class OSProvider {
    private final OSSettings settings;
    private final Config config;

    @Produces
    public OS getOS() {
        return settings.getOS(config);
    }

}

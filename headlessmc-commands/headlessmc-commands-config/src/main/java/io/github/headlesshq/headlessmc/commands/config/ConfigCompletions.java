package io.github.headlesshq.headlessmc.commands.config;

import io.github.headlesshq.headlessmc.config.ConfigService;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;

import java.util.Iterator;

@Default
@Dependent
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Named("headlessmc:completions:config")
public class ConfigCompletions implements Iterable<String> {
    private final ConfigService configService;

    @Override
    public Iterator<String> iterator() {
        return configService.getPropertyNames().stream()
            .filter(property -> property.startsWith("hmc."))
            .iterator();
    }

}

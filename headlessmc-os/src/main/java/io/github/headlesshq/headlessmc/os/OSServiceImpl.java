package io.github.headlesshq.headlessmc.os;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Unmodifiable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;
import java.util.stream.Collectors;

@ApplicationScoped
public class OSServiceImpl implements OSService {
    private final Instance<OSConfigurator> configurators;
    private final Instance<CPU> cpu;
    private final Instance<OS> os;

    @Inject
    public OSServiceImpl(@Any Instance<OSConfigurator> configurators, @Any Instance<CPU> cpu, @Any Instance<OS> os) {
        this.configurators = configurators;
        this.cpu = cpu;
        this.os = os;
    }

    @Override
    @Produces
    @Default
    @Dependent
    public OS getOS() {
        SequencedSet<OS> candidates = os.handlesStream() // avoid StackOverflow, because Instance also contains this
            .filter(handle -> !handle.getBean().getBeanClass().isAssignableFrom(getClass()))
            .map(Instance.Handle::get)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        OS result = candidates.getFirst();
        for (OSConfigurator configurator : configurators.stream().toList()) {
            result = configurator.configure(result, candidates);
            candidates.addFirst(result);
        }

        return result;
    }

    @Override
    @Produces
    @Default
    @Dependent
    public CPU getCPU() {
        SequencedSet<CPU> candidates = cpu.handlesStream() // avoid StackOverflow, because Instance also contains this
            .filter(handle -> !handle.getBean().getBeanClass().isAssignableFrom(getClass()))
            .map(Instance.Handle::get)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        CPU result = candidates.getFirst();
        for (OSConfigurator configurator : configurators.stream().toList()) {
            result = configurator.configure(result, candidates);
            candidates.addFirst(result);
        }

        return result;
    }

    @Override
    @Unmodifiable
    public List<OSConfigurator> getConfigurators() {
        return configurators.stream().toList();
    }

}

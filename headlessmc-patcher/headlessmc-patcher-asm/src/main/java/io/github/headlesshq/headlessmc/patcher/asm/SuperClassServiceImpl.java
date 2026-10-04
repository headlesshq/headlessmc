package io.github.headlesshq.headlessmc.patcher.asm;

import io.github.headlesshq.headlessmc.patcher.PatchContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Default implementation of {@link SuperClassService}.
 */
@ApplicationScoped
public class SuperClassServiceImpl implements SuperClassService {
    private final Instance<SuperClassStrategy> strategies;

    @Inject
    public SuperClassServiceImpl(@Any Instance<SuperClassStrategy> strategies) {
        this.strategies = strategies;
    }

    @Override
    public List<SuperClassStrategy> getStrategies() {
        return strategies.stream().toList();
    }

    @Override
    public SuperClassResolver resolver(PatchContext context) {
        return new AggregateSuperClassResolver(strategies.stream().toList(), context);
    }

}

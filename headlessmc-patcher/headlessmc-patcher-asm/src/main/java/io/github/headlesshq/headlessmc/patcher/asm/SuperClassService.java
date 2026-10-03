package io.github.headlesshq.headlessmc.patcher.asm;

import io.github.headlesshq.headlessmc.patcher.HelperService;
import io.github.headlesshq.headlessmc.patcher.PatchContext;

import java.util.List;

/**
 * Manages and provides {@link SuperClassStrategy} instances.
 */
public interface SuperClassService extends HelperService {
    /**
     * @return all {@link SuperClassStrategy} implementations available.
     */
    List<SuperClassStrategy> getStrategies();

    /**
     * Returns an aggregated {@link SuperClassResolver} that attempts to
     * resolve super classes with any available {@link SuperClassStrategy}.
     *
     * @param context the patching context for which to resolve super classes.
     * @return an aggregated {@link SuperClassResolver}
     * over all available {@link SuperClassStrategy} instances.
     */
    SuperClassResolver resolver(PatchContext context);

}

package io.github.headlesshq.headlessmc.patcher.asm;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.patcher.PatchContext;
import io.github.headlesshq.headlessmc.patcher.PatchException;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassWriter;

import java.util.List;

@Slf4j
final class EntryClassWriter extends ClassWriter {
    private final List<SuperClassStrategy> superClassStrategies;
    private final PatchContext context;

    public EntryClassWriter(PatchContext context) {
        super(COMPUTE_FRAMES);
        this.context = context;
        this.superClassStrategies = context.services(SuperClassService.class)
            .map(SuperClassService::getStrategies)
            .flatMap(List::stream)
            .toList();
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        PatchException exception = new PatchException("Failed to find common super class of " + type1 + ", " + type2);
        for (SuperClassStrategy strategy : superClassStrategies) {
            if (strategy.isApplicable()) {
                try (SuperClassResolver resolver = strategy.apply(context)) {
                    return resolver.getCommonSuperClass(type1, type2);
                } catch (HeadlessMcException e) {
                    log.info("CommonSuperClassStrategy {} failed", strategy, e);
                    exception.addSuppressed(e);
                }
            }
        }

        throw exception;
    }

}

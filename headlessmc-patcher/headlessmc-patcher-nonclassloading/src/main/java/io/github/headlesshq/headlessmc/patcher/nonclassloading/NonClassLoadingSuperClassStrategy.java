package io.github.headlesshq.headlessmc.patcher.nonclassloading;

import io.github.headlesshq.headlessmc.patcher.PatchContext;
import io.github.headlesshq.headlessmc.patcher.asm.SuperClassResolver;
import io.github.headlesshq.headlessmc.patcher.asm.SuperClassStrategy;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * A {@link SuperClassStrategy} that does not need to load classes,
 * but instead reads the bytecode of the jars on the classpath,
 * to find out the type hierarchy.
 *
 * @see <a href=https://github.com/Grundlefleck/ASM-NonClassloadingExtensions>
 * https://github.com/Grundlefleck/ASM-NonClassloadingExtensions</a>
 */
@Dependent
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class NonClassLoadingSuperClassStrategy implements SuperClassStrategy {
    private final ClassSourceFactory classSourceFactory;

    @Override
    public SuperClassResolver apply(PatchContext context) {
        List<ClassSource.Provider> providers = classSourceFactory.create(context);
        return new NonClassLoadingSuperClassResolver(providers);
    }

    @Override
    public boolean isApplicable() {
        return true;
    }

    @Override
    public int sort() {
        return SORT_NON_CLASS_LOADING;
    }

}

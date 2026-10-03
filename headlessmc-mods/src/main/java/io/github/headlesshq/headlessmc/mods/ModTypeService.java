package io.github.headlesshq.headlessmc.mods;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import java.util.Optional;
import java.util.Set;

/**
 * Provides the set of all existing {@link ModType}s.
 * When you are creating an own ModType,
 * you need to add a {@link Named} {@link Produces}
 * method that provides your ModType for it
 * to show up in this service:
 * <pre>
 * {@code
 *     @Produces
 *     @Dependent
 *     @Named("mod:type:modpack")
 *     public ModType getModpackType() {
 *         return ModType.MOD_PACK;
 *     }
 * }
 * </pre>
 */
public interface ModTypeService {
    /**
     * The set of all known {@link ModType}s.
     *
     * @return the set of all known ModTypes.
     */
    Set<ModType> getAllModTypes();

    Optional<ModType> getByName(String name);

}

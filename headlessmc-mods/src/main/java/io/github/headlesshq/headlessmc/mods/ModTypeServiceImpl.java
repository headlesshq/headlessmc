package io.github.headlesshq.headlessmc.mods;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ModTypeService}.
 */
@ApplicationScoped
public class ModTypeServiceImpl implements ModTypeService {
    private final Instance<ModType> modTypes;

    @Inject
    public ModTypeServiceImpl(@Any Instance<ModType> modTypes) {
        this.modTypes = modTypes;
    }

    @Override
    public Set<ModType> getAllModTypes() {
        return modTypes.stream().collect(Collectors.toSet());
    }

    @Override
    public Optional<ModType> getByName(String name) {
        return getAllModTypes()
            .stream()
            .filter(type -> type.name().equalsIgnoreCase(name))
            .findFirst();
    }

    @Produces
    @Dependent
    @Named("mod:type:mod")
    public ModType getModType() {
        return ModType.MOD;
    }

    @Produces
    @Dependent
    @Named("mod:type:plugin")
    public ModType getPluginType() {
        return ModType.PLUGIN;
    }

    @Produces
    @Dependent
    @Named("mod:type:resourcepack")
    public ModType getResourcepackType() {
        return ModType.RESOURCE_PACK;
    }

    @Produces
    @Dependent
    @Named("mod:type:datapack")
    public ModType getDatapackType() {
        return ModType.DATA_PACK;
    }

    @Produces
    @Dependent
    @Named("mod:type:modpack")
    public ModType getModpackType() {
        return ModType.MOD_PACK;
    }

    @Produces
    @Dependent
    @Named("mod:type:shaderpack")
    public ModType getShaderType() {
        return ModType.SHADER;
    }

}

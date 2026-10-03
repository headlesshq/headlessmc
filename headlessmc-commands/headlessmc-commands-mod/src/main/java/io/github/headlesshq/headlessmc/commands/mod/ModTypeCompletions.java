package io.github.headlesshq.headlessmc.commands.mod;

import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.mods.ModTypeService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;

import java.util.Iterator;

@Default
@Dependent
@RegisterForReflection
@Named("headlessmc:picocli:completions:mod:types") // prevent Bean from getting removed by Quarkus during build
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ModTypeCompletions implements Iterable<String> {
    private final ModTypeService typeService;

    @Override
    public Iterator<String> iterator() {
        return typeService.getAllModTypes().stream()
            .map(ModType::name)
            .distinct()
            .iterator();
    }

}

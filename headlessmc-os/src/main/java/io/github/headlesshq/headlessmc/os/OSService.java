package io.github.headlesshq.headlessmc.os;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public interface OSService {
    @Produces
    @Default
    @Dependent
    OS getOS();

    @Produces
    @Default
    @Dependent
    CPU getCPU();

    @Unmodifiable
    List<OSConfigurator> getConfigurators();

}

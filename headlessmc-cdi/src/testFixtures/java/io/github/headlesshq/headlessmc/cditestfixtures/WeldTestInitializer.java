package io.github.headlesshq.headlessmc.cditestfixtures;

import io.github.headlesshq.headlessmc.cdi.CDIInitializer;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.junit5.WeldInitiator;

import java.util.List;

/**
 * This builds a {@link WeldInitiator.Builder} powered by {@link CDIInitializer}
 * to have all packages needed.
 */
public class WeldTestInitializer {
    public static WeldInitiator.Builder getInitiatorBuilder() {
        return WeldInitiator.from(getWeld());
    }

    public static Weld getWeld(Class<?>... additionalPackagesToScan) {
        CDIInitializer cdiInitializer = new CDIInitializer();
        List<Class<?>> packages = cdiInitializer.gatherPackages();
        Weld weld = new Weld();
        packages.forEach(pkg -> weld.addPackage(true, pkg));
        for (Class<?> clazz : additionalPackagesToScan) {
            weld.addPackage(true, clazz);
        }

        return weld;
    }

}

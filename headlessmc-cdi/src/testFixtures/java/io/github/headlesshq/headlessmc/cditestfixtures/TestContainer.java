package io.github.headlesshq.headlessmc.cditestfixtures;

import io.github.headlesshq.headlessmc.cdi.CDIInitializer;
import jakarta.enterprise.inject.se.SeContainer;

public class TestContainer {
    private static volatile SeContainer container;

    public static SeContainer getContainer() {
        if (container == null) {
            synchronized (TestContainer.class) {
                if (container == null) {
                    container = new CDIInitializer().create();
                }
            }
        }

        return container;
    }

}

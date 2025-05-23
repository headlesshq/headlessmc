package io.github.headlessmc.headlesshq.weld;

import io.github.headlesshq.headlessmc.api.Application;
import io.github.headlesshq.headlessmc.jline.JLineCommandLineReaderProvider;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import org.jboss.logging.Logger;

public class WeldTest {
    public static void main(String[] args) {
        System.setProperty("org.jboss.logging.Logger.level", "ALL");

        SeContainerInitializer initializer = SeContainerInitializer.newInstance();
        initializer.setClassLoader(WeldTest.class.getClassLoader());
        initializer.addPackages(true, Application.class, WeldTest.class, JLineCommandLineReaderProvider.class);
        try (SeContainer container = initializer.initialize()) {
            Application application = container.select(Application.class).get();
            // Use application...
        }
    }

}

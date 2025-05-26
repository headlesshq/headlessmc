package io.github.headlesshq.headlessmc.testjline;

import io.github.headlesshq.headlessmc.api.Application;
import io.github.headlesshq.headlessmc.jline.JLineCommandLineReader;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;

public class Main {
    public static void main(String[] args) {
        SeContainerInitializer initializer = SeContainerInitializer.newInstance();
        initializer.setClassLoader(Application.class.getClassLoader());
        initializer.addPackages(true, Application.class);
        initializer.addPackages(true, JLineCommandLineReader.class);
        //initializer.addBeanClasses(Observer.class);
        // TODO: add more packages?!

        initializer.addPackages(true, Application.class);
        try (SeContainer container = initializer.initialize()) {
            Application application = container.select(Application.class).get();
            if (args.length == 0) {
                application.getCommandLine().setInteractiveContext(application.getCommandLine().getContext());
                application.getCommandLine().getReader().read(application);
            } else {
                application.getCommandLine().getContext().executeArgs(args);
            }
        }
    }

}

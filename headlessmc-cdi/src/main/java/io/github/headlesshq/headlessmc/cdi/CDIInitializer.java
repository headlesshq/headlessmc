package io.github.headlesshq.headlessmc.cdi;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class CDIInitializer {
    public SeContainer create() {
        SeContainerInitializer initializer = SeContainerInitializer.newInstance();
        gatherPackages().forEach(clazz -> initializer.addPackages(true, clazz));
        return initializer.initialize();
    }

    public List<Class<?>> gatherPackages() {
        List<Class<?>> classes = new ArrayList<>();
        ServiceLoader.load(PackageProvider.class).forEach(pp -> classes.add(pp.getPackageClass()));
        return classes;
    }

}

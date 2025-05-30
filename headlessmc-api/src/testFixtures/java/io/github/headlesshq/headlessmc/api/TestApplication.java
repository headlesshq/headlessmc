package io.github.headlesshq.headlessmc.api;

import io.github.headlesshq.headlessmc.api.cdi.PackageProvider;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import lombok.SneakyThrows;

import java.util.ServiceLoader;

public class TestApplication {
    @SneakyThrows
    public static void use(ThrowingConsumer<Application> action) {
        SeContainerInitializer initializer = SeContainerInitializer.newInstance();
        initializer.setClassLoader(Application.class.getClassLoader());
        ServiceLoader<PackageProvider> serviceLoader = ServiceLoader.load(PackageProvider.class);
        for (PackageProvider provider : serviceLoader) {
            initializer.addPackages(true, provider.getPackageClass());
        }

        // I think this is needed because the api package is split between src/main and src/test
        initializer.addPackages(true, TestApplication.class.getPackage());
        try (SeContainer container = initializer.initialize()) {
            Application application = container.select(Application.class).get();
            action.accept(application);
        }
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void accept(T t) throws Throwable;
    }

}

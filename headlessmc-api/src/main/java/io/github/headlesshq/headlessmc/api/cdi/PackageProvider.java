package io.github.headlesshq.headlessmc.api.cdi;

@FunctionalInterface
public interface PackageProvider {
    Class<?> getPackageClass();

}

package io.github.headlesshq.headlessmc.cdi;

@FunctionalInterface
public interface PackageProvider {
    Class<?> getPackageClass();

}

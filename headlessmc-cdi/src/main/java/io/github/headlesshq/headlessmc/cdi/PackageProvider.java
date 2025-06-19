package io.github.headlesshq.headlessmc.cdi;

import jakarta.inject.Inject;

/**
 * HeadlessMc makes use of the package scanning mechanism in CDI.
 * To discover all packages to recursively scan, we use this SPI,
 * which is loaded by a {@link java.util.ServiceLoader}.
 * It is important to note that PackageProviders are loaded
 * before the CDI Container and thus are not Beans.
 * They cannot {@link Inject @Inject} any beans.
 * A sample implementation:
 * <pre>
 * {@code
 * @AutoService(MyPackageProvider.class)
 * public final class MyPackageProvider implements PackageProvider {
 *     @Override
 *     public Class<?> getPackageClass() {
 *         return MyPackageProvider.class;
 *     }
 * }}</pre>
 * Any application wishing to be discovered by HeadlessMc
 * should return a class from its root package to discover
 * all beans inside its packages.
 * If a package is split, multiple package providers should probably be used.
 * E.g. between src/main/java and src/main/test.
 * Either use {@link com.google.auto.service.AutoService} or
 * follow the instructions for a {@link java.util.ServiceLoader}
 * to allow it to discover your PackageProvider.
 */
@FunctionalInterface
public interface PackageProvider {
    /**
     * Returns a class in a package that should be recursively scanned.
     * Can be the class of this PackageProvider.
     *
     * @return a class inside the root package of the application. Can be this class.
     */
    Class<?> getPackageClass();

}

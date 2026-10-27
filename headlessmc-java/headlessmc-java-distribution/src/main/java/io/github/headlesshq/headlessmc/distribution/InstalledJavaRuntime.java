package io.github.headlesshq.headlessmc.distribution;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jspecify.annotations.Nullable;

/**
 * JSON format of the {@link InstalledJavaRuntimeService#FILE_NAME} file, which records which {@link JavaRuntime}
 * has been installed into a directory. Without it, we would only know the major version of an installation,
 * but not its provider, distribution and full version, which are needed to check it for updates.
 * The file wraps this object with its {@link InstalledJavaRuntimeService#FORMAT_VERSION}:
 * <pre>
 * {@code
 * {
 *   "version": 1,
 *   "object": {
 *     "provider": "foojay.io",
 *     "distribution": "temurin",
 *     "id": "88fb1303ff6928587c165c897f5e57d8",
 *     "name": "OpenJDK25U-jre_x64_linux_hotspot_25_36",
 *     "majorVersion": 25,
 *     "javaVersion": "25+36",
 *     "packageType": "jre",
 *     "archiveType": "tar.gz"
 *   }
 * }
 * }
 * </pre>
 *
 * @param provider     {@link JavaRuntime#provider()}.
 * @param distribution {@link JavaRuntime#distribution()}.
 * @param id           {@link JavaRuntime#id()}.
 * @param name         {@link JavaRuntime#name()}.
 * @param majorVersion {@link JavaRuntime#version()}.
 * @param javaVersion  {@link JavaRuntime.MetaData#javaVersion()}.
 * @param packageType  {@link JavaRuntime.MetaData#packageType()}.
 * @param archiveType  {@link JavaRuntime.MetaData#archiveType()}.
 */
@RegisterForReflection
public record InstalledJavaRuntime(
    @Nullable String provider,
    @Nullable String distribution,
    @Nullable String id,
    @Nullable String name,
    int majorVersion,
    @Nullable String javaVersion,
    @Nullable String packageType,
    @Nullable String archiveType
) implements ReflectionRegistered {
    public static InstalledJavaRuntime of(JavaRuntime runtime) {
        return new InstalledJavaRuntime(
            runtime.provider(),
            runtime.distribution(),
            runtime.id(),
            runtime.name(),
            runtime.version(),
            runtime.javaVersion(),
            runtime.packageType(),
            runtime.metaData() == null ? null : runtime.metaData().archiveType()
        );
    }

    /**
     * @return the {@link JavaRuntime} described by this object.
     * @throws JavaDistributionException if a required field is missing.
     */
    public JavaRuntime toRuntime() throws JavaDistributionException {
        if (provider == null || distribution == null || id == null || name == null || majorVersion <= 0) {
            throw new JavaDistributionException("Incomplete installed java runtime " + this);
        }

        return new JavaRuntime(
            provider,
            distribution,
            id,
            name,
            majorVersion,
            new JavaRuntime.MetaData(null, archiveType, javaVersion, packageType)
        );
    }

}

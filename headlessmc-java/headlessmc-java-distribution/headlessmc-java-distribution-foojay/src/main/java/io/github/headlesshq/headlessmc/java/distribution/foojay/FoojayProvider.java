package io.github.headlesshq.headlessmc.java.distribution.foojay;

import eu.hansolo.jdktools.Architecture;
import eu.hansolo.jdktools.Bitness;
import eu.hansolo.jdktools.LibCType;
import eu.hansolo.jdktools.OperatingSystem;
import io.github.headlesshq.headlessmc.distribution.JavaDistribution;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionException;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProvider;
import io.github.headlesshq.headlessmc.distribution.JavaRuntime;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.net.DownloadService;
import io.github.headlesshq.headlessmc.net.rest.ApiException;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.LibC;
import io.github.headlesshq.headlessmc.os.LibCDetector;
import io.github.headlesshq.headlessmc.os.OS;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class FoojayProvider implements JavaDistributionProvider {
    /**
     * Without this the foojay API interprets a major version like {@code 21} as exactly {@code 21+35}.
     */
    static final String LATEST_AVAILABLE = "available";
    /**
     * Only general availability releases, no early access builds.
     */
    static final String RELEASE_STATUS_GA = "ga";

    private final FoojayInstallerService archiveService;
    private final DownloadService downloadService;
    private final @RestClient FoojayAPI api;
    private final LibCDetector libCDetector;

    @Override
    public String getName() {
        return JavaDistributionProvider.DEFAULT;
    }

    @Override
    public List<JavaDistribution> getDistributions() throws JavaDistributionException {
        try {
            return api.getDistributions()
                .stream()
                .map(distribution -> distribution.toJavaDistribution(getName()))
                .toList();
        } catch (ApiException | ProcessingException e) {
            throw new JavaDistributionException(e);
        }
    }

    @Override
    public JavaDistribution getDistributionByName(String name) throws HeadlessMcException {
        try {
            return api.getDistribution(name).toJavaDistribution(getName());
        } catch (ApiException | ProcessingException e) {
            throw new JavaDistributionException(e);
        }
    }

    @Override
    public List<JavaRuntime> getRuntimes(
        JavaDistribution distribution,
        int version,
        OS os,
        CPU cpu
    ) throws JavaDistributionException {
        Bitness bitness = FoojayUtil.cpu2Bitness(cpu);
        Architecture architecture = FoojayUtil.cpu2Architecture(cpu);
        OperatingSystem operatingSystem = FoojayUtil.os2OperatingSystem(os);

        try {
            return api.packages(
                    String.valueOf(version),
                    List.of(distribution.id()),
                    List.of(operatingSystem.getApiString()),
                    List.of(architecture.getApiString()),
                    bitness.getAsInt(),
                    archiveService.getSupportedArchiveTypes(),
                    null,
                    null, // true?
                    List.of(libCType(os, operatingSystem).getApiString()),
                    LATEST_AVAILABLE,
                    List.of(RELEASE_STATUS_GA),
                    false
                )
                .stream()
                .map(pkg -> pkg.toRuntime(getName(), version, distribution.id()))
                .toList();
        } catch (ApiException | ProcessingException e) {
            throw new JavaDistributionException(e);
        }
    }

    /**
     * Determines the C library the Java runtimes have to be built against.
     * On {@link OS.Type#LINUX} the C library of the host is detected, because e.g. a musl system
     * can only run Java runtimes built against musl, even if the OS has not been detected as {@link OS.Type#LINUX_MUSL}.
     * An OS that is explicitly {@link OS.Type#LINUX_MUSL} always uses musl.
     *
     * @param os              the operating system to find Java runtimes for.
     * @param operatingSystem the Foojay operating system for the given OS.
     * @return the C library of the host, or the default C library of the operating system if it cannot be detected.
     */
    private LibCType libCType(OS os, OperatingSystem operatingSystem) {
        if (OS.Type.LINUX_MUSL.equals(os.type())) {
            return LibCType.MUSL;
        }

        if (OS.Type.LINUX.equals(os.type())) {
            LibC libC = libCDetector.detectSystem();
            if (libC == LibC.MUSL) {
                return LibCType.MUSL;
            } else if (libC == LibC.GLIBC) {
                return LibCType.GLIBC;
            }
        }

        return operatingSystem.getLibCType();
    }

    @Override
    public Optional<JavaRuntime> getUpdate(JavaRuntime runtime, OS os, CPU cpu) throws JavaDistributionException {
        JavaDistribution distribution = new JavaDistribution(getName(), runtime.distribution(), runtime.distribution());
        String packageType = runtime.packageType();
        return getRuntimes(distribution, runtime.version(), os, cpu)
            .stream()
            .filter(candidate -> packageType == null || packageType.equalsIgnoreCase(candidate.packageType()))
            .findFirst()
            .filter(latest -> latest.javaVersion() == null || !latest.javaVersion().equals(runtime.javaVersion()));
    }

    @Override
    public void download(JavaRuntime runtime, Path path) throws HeadlessMcException {
        try {
            PkgInfo pkgInfo = api.pkgInfo(runtime.id());
            if (pkgInfo.direct_download_uri() == null) {
                throw new JavaDistributionException("Failed to get download URL for " + runtime + ", API: " + pkgInfo);
            }

            JavaRuntime.MetaData metaData = runtime.metaData();
            if (metaData == null || metaData.archiveType() == null) {
                throw new JavaDistributionException("Failed to get distribution meta data " + runtime);
            }

            URI uri = URI.create(pkgInfo.direct_download_uri());
            downloadService.download(uri)
                .progressBar("Downloading Java " + runtime.distribution() + " " + runtime.version())
                .size(metaData.size())
                .hash(pkgInfo.getHashAlgorithmName(), pkgInfo.checksum())
                .start(download -> archiveService.install(path, metaData, download.getInputStream()));
        } catch (ApiException | ProcessingException e) {
            throw new JavaDistributionException(e);
        }
    }

}

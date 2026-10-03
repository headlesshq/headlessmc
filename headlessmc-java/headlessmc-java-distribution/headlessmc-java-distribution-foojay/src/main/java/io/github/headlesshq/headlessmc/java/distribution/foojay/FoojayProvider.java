package io.github.headlesshq.headlessmc.java.distribution.foojay;

import eu.hansolo.jdktools.Architecture;
import eu.hansolo.jdktools.Bitness;
import eu.hansolo.jdktools.OperatingSystem;
import io.github.headlesshq.headlessmc.distribution.JavaDistribution;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionException;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProvider;
import io.github.headlesshq.headlessmc.distribution.JavaRuntime;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.net.DownloadService;
import io.github.headlesshq.headlessmc.net.rest.ApiException;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class FoojayProvider implements JavaDistributionProvider {
    private final FoojayInstallerService archiveService;
    private final DownloadService downloadService;
    private final @RestClient FoojayAPI api;

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
                    List.of(operatingSystem.getLibCType().getApiString())
                )
                .stream()
                .map(pkg -> pkg.toRuntime(getName(), version, distribution.id()))
                .toList();
        } catch (ApiException | ProcessingException e) {
            throw new JavaDistributionException(e);
        }
    }

    @Override
    public List<JavaRuntime> getUpdates(JavaRuntime runtime) throws JavaDistributionException {
        //List<JavaRuntime> runtimes = getRuntimes(runtime.distribution())
        // use SemVer
        throw new UnsupportedOperationException(); // TODO?
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

package io.github.headlesshq.headlessmc.java.sources;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaConfig;
import io.github.headlesshq.headlessmc.java.JavaSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;

import java.util.List;

@ApplicationScoped
@Named("java:source:headlessmc")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class HeadlessMcJavaSource implements JavaSource {
    private final JavaScannerService javaScannerService;
    private final Holder<JavaConfig> config;
    private final AppFiles appFiles;

    @Override
    public String getName() {
        return "headlessmc";
    }

    @Override
    public List<Java> getJavas() {
        // actually this should log if a directory in scanDir has no executable
        // every directory in the java dir is an installation, but the java home inside it may be
        // nested, e.g. on macOS: <installation>/jdk8u502-b07-jre/Contents/Home
        return javaScannerService.scanSubDirsOf(this, appFiles.getJavaDir(), config.get().maxScanDepth());
    }

    @Override
    public int sort() {
        return JavaSource.SORT_HMC;
    }

}

package io.github.headlesshq.headlessmc.java.sources;

import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaSource;
import io.github.headlesshq.headlessmc.java.SafePath;
import io.github.headlesshq.headlessmc.java.version.SpecificationVersionParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;

@ApplicationScoped
@Named("java:source:current")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class CurrentJavaSource implements JavaSource {
    private final SpecificationVersionParser specificationVersionParser;
    private final JavaExecutableFinder executableFinder;

    @Override
    public String getName() {
        return "current";
    }

    @Override
    public List<Java> getJavas() {
        String javaHome = System.getProperty("java.home");
        Path home = javaHome == null ? null : FileSystems.getDefault().getPath(javaHome);
        SafePath safeHome = new SafePath(home);
        String specificationVersion = System.getProperty("java.specification.version");
        int version = specificationVersionParser.parseSpecificationVersion(specificationVersion);
        SafePath executable = safeHome.map(executableFinder::getExecutable);
        String name = home == null ? "current" : home.getFileName().toString();

        return List.of(new Java(name, version, safeHome, executable, true, sort()));
    }

    @Override
    public int sort() {
        return JavaSource.SORT_CURRENT;
    }

}

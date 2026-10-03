package io.github.headlesshq.headlessmc.java.sources;

import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaSource;
import io.github.headlesshq.headlessmc.os.OS;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@ApplicationScoped
@Named("java:source:linux")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class LinuxJavaSource implements JavaSource {
    private final JavaScannerService javaScannerService;
    private final OS os;

    @Override
    public String getName() {
        return OS.Type.LINUX.name();
    }

    @Override
    public List<Java> getJavas() {
        if (!OS.McType.LINUX.equals(os.type().mcType())) {
            return List.of();
        }

        List<Java> result = new ArrayList<>();
        Iterable<Path> rootDirectories = FileSystems.getDefault().getRootDirectories();
        for (Path rootPath : rootDirectories) {
            result.addAll(javaScannerService.scanSubDirsOf(this, rootPath.resolve("usr").resolve("lib").resolve("jvm")));
            result.addAll(javaScannerService.scanSubDirsOf(this, rootPath.resolve("usr").resolve("local")));
        }

        return result;
    }

    @Override
    public int sort() {
        return JavaSource.SORT_GENERAL;
    }

}

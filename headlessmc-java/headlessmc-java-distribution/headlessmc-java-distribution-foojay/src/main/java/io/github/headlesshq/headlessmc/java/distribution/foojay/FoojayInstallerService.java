package io.github.headlesshq.headlessmc.java.distribution.foojay;

import eu.hansolo.jdktools.ArchiveType;
import io.github.headlesshq.headlessmc.distribution.JavaRuntime;
import io.github.headlesshq.headlessmc.exceptions.FileException;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.java.sources.JavaExecutableFinder;
import io.github.headlesshq.headlessmc.java.sources.JavaHomeFinder;
import io.github.headlesshq.headlessmc.os.OS;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
class FoojayInstallerService {
    private final Map<ArchiveType, Extractor> extractors = Map.of(
        // ArchiveType.APK, new Extractor("apk", null),
        ArchiveType.TAR, new Extractor("tar", null),
        ArchiveType.TAR_GZ, new Extractor("tar", "gz"),
        ArchiveType.TGZ, new Extractor("tar", "gz"),
        ArchiveType.TAR_Z, new Extractor("tar", "z"),
        ArchiveType.ZIP, new Extractor("zip", null)
    );

    private final JavaExecutableFinder executableFinder;
    private final JavaHomeFinder homeFinder;
    private final FileService fileService;
    private final OS os;

    public List<ArchiveType> getSupportedArchiveTypes() {
        return new ArrayList<>(extractors.keySet());
    }

    public void install(Path path, JavaRuntime.MetaData metaData, InputStream inputStream) throws FileException {
        ArchiveType type = ArchiveType.fromApiString(metaData.archiveType());
        if (type == null) {
            throw new FileException("Unknown archive type " + metaData.archiveType());
        }

        Extractor extractor = extractors.get(type);
        if (extractor == null) {
            throw new FileException("Unsupported Archive Type: %s (%s)".formatted(metaData.archiveType(), type));
        }

        fileService.atomic(
            path, workingDir -> {
                extractor.extract(workingDir, inputStream);
                // the archive can be deeply nested
                // e.g. on macOS: OpenJDK8U-jre_x64_mac_hotspot_8u502b07/jdk8u502-b07-jre/Contents/Home
                Path javaHome = homeFinder.find(workingDir).orElseThrow(
                    () -> new FileException("Failed to find installation dir in " + workingDir)
                );

                addPermissions(javaHome, executableFinder.getExecutable(javaHome));
                // ensure version can be parsed?
            }
        );
    }

    private void addPermissions(Path javaHome, Path executable) throws FileException {
        if (OS.Type.WINDOWS.equals(os.type())) { // enough? Other OS we should not do this on?
            return;
        }

        Set<PosixFilePermission> executePermissions = new HashSet<>();
        executePermissions.add(PosixFilePermission.OWNER_READ);
        executePermissions.add(PosixFilePermission.OWNER_EXECUTE);
        executePermissions.add(PosixFilePermission.GROUP_READ);
        executePermissions.add(PosixFilePermission.GROUP_EXECUTE);
        executePermissions.add(PosixFilePermission.OTHERS_READ);
        executePermissions.add(PosixFilePermission.OTHERS_EXECUTE); // ?
        try {
            Files.setPosixFilePermissions(executable, executePermissions);

            Path jspawnhelper = javaHome.resolve("lib").resolve("jspawnhelper");
            if (Files.exists(jspawnhelper)) {
                // https://askubuntu.com/a/1492514
                Files.setPosixFilePermissions(jspawnhelper, executePermissions);
            }
        } catch (IOException e) {
            throw new FileException(e);
        }
    }

}

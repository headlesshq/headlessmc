package io.github.headlesshq.headlessmc.platform.util;

import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.version.VersionParser;
import io.github.headlesshq.headlessmc.version.service.VersionJsonService;
import io.github.headlesshq.headlessmc.version.service.VersionJsonServiceImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class VersionJsonServiceFactory {
    @Produces
    @ApplicationScoped
    public VersionJsonService versionJsonService(VersionParser versionParser, McFilesService mcFilesService) {
        return new VersionJsonServiceImpl(
            versionParser,
            () -> mcFilesService.getAllMcFiles().stream().map(McFiles::getVersionsDir).toList()
        );
    }
    
}

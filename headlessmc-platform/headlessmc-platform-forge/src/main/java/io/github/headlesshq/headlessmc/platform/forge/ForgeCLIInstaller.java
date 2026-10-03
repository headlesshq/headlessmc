package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.java.launcher.JavaLauncherService;
import io.github.headlesshq.headlessmc.java.launcher.ProcessHandler;
import io.quarkus.runtime.annotations.RegisterResources;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@RegisterResources(globs = {ForgeCLIInstaller.RESOURCE_NAME})
public class ForgeCLIInstaller {
    static final String RESOURCE_NAME = "forge/headlessmc-forge-installer.jar";
    static final String MAIN_CLASS = "io.github.headlesshq.headlessmc.forge.installer.Main";

    private final JavaLauncherService javaLauncher;
    private final FileService fileService;

    public void installClient(Path installerJar, Path installationDir, int javaVersion) throws HeadlessMcException {
        fileService.temp(temp -> {
            Path forgeCli = temp.resolve(RESOURCE_NAME);
            if (!Files.exists(forgeCli)) {
                fileService.extractResource(RESOURCE_NAME, forgeCli);
            }

            fileService.ensureFileExists(
                installationDir.resolve("launcher_profiles.json"),
                "{\"profiles\": {}}".getBytes(StandardCharsets.UTF_8)
            );

            fileService.ensureFileExists(
                installationDir.resolve("launcher_profiles_microsoft_store.json"),
                "{\"profiles\": {}}".getBytes(StandardCharsets.UTF_8)
            );

            javaLauncher.buildProcess()
                .id("forge-cli-installer")
                .mainClass(MAIN_CLASS)
                .version(javaVersion)
                .directory(installationDir)
                .classpath(forgeCli, installerJar)
                .arg(installationDir.toAbsolutePath().toString())
                .start()
                .waitFor(ProcessHandler.defaultHandler());
        });
    }

}

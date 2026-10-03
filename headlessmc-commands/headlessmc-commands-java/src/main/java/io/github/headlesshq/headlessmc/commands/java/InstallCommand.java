package io.github.headlesshq.headlessmc.commands.java;

import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.distribution.JavaDistribution;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProvider;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProviderService;
import io.github.headlesshq.headlessmc.distribution.JavaRuntime;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.java.JavaService;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Getter
@Setter
@Default
@Dependent
@Named("command:java:install")
@CommandLine.Command(
    name = "install",
    aliases = "download",
    mixinStandardHelpOptions = true,
    description = "Downloads and installs a java version."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class InstallCommand implements Runnable, CachedConsole.Enabled {
    private final JavaDistributionProviderService distributionServiceManager;
    private final FileService fileService;
    private final JavaService javaService;
    private final AppFiles appFiles;
    private final Console console;
    private final OS os;
    private final CPU cpu;

    @CommandLine.ParentCommand
    JavaCommand context;

    @CommandLine.Option(
        names = {"-f", "--force"},
        description = "Forces reinstallation even if the java version is already installed.",
        defaultValue = "false"
    )
    private boolean force;

    @CommandLine.Parameters(description = "The major java version to install, e.g. 21.")
    private int version;

    @Override
    public void run() {
        JavaDistributionProvider service = context.getService();
        JavaDistribution javaDistribution = context.getDistribution(service);

        List<JavaRuntime> runtimes = service.getRuntimes(javaDistribution, version, os, cpu);
        if (runtimes.isEmpty()) {
            throw new IllegalArgumentException(
                "Failed to find a java %s runtime for distribution %s on %s/%s".formatted(
                    version, javaDistribution.name(), os.name(), cpu.architecture()
                )
            );
        }

        JavaRuntime runtime = runtimes.getFirst();
        Path path = appFiles.getJavaDir().resolve(runtime.name());
        if (Files.exists(path) && !force) {
            throw new IllegalArgumentException(
                "Java installation " + runtime.name() + " already exists, use --force to reinstall it."
            );
        }

        service.download(runtime, path);
        javaService.refresh();
        console.write("Installed java " + version + " (" + javaDistribution.name() + ") to " + path);
    }

}

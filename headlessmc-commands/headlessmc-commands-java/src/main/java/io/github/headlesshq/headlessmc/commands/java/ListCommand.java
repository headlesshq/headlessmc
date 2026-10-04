package io.github.headlesshq.headlessmc.commands.java;

import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.distribution.JavaDistribution;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProvider;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProviderService;
import io.github.headlesshq.headlessmc.distribution.JavaRuntime;
import io.github.headlesshq.headlessmc.java.Java;
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
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.util.List;

@Getter
@Setter
@Default
@Dependent
@Named("command:java:list")
@CommandLine.Command(
    name = "list",
    aliases = "ls",
    mixinStandardHelpOptions = true,
    description = "Lists installed or remote java installations."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ListCommand implements Runnable, CachedConsole.Enabled {
    private final JavaDistributionProviderService distributionServiceManager;
    private final TableProvider tableProvider;
    private final JavaService javaService;
    private final Console console;
    private final OS os;
    private final CPU cpu;

    @CommandLine.ParentCommand
    JavaCommand context;

    @CommandLine.Option(
        names = {"-r", "--remote"},
        description = "Lists remote distributions (or, if a version is given, installable runtimes).",
        defaultValue = "false"
    )
    private boolean remote;

    @CommandLine.Option(
        names = {"--providers"},
        description = "Lists the available providers.",
        defaultValue = "false"
    )
    private boolean providers;

    @CommandLine.Parameters(
        description = "The major java version to list remote runtimes for, e.g. 21.",
        arity = "0..1"
    )
    private @Nullable Integer version;

    @Override
    public void run() {
        if (providers) {
            listProviders();
            if (remote) {
                listRemote();
            }

            return;
        }

        if (remote) {
            listRemote();
            return;
        }

        javaService.refresh();
        tableProvider.<Java>get()
            .withColumn("name", Java::name)
            .withColumn("version", java -> String.valueOf(java.version()))
            .withColumn("home", java -> java.home().toString())
            .withStringValueOf("current", java -> java.current() ? "<------" : "       ")
            .addAll(javaService.getJavaVersions())
            .log(console::write);
    }

    private void listRemote() {
        if (version == null) {
            listDistributions();
            return;
        }

        listRuntimes(version);
    }

    private void listProviders() {
        tableProvider.<JavaDistributionProvider>get()
            .withColumn("name", JavaDistributionProvider::getName)
            .addAll(distributionServiceManager.getProviders())
            .log(console::write);
    }

    private void listDistributions() {
        List<JavaDistributionProvider> providers = context.getProvider() == null
            ? distributionServiceManager.getProviders()
            : List.of(context.getService());

        tableProvider.<JavaDistribution>get()
            .withColumn("name", JavaDistribution::name)
            .withColumn("id", JavaDistribution::id)
            .withColumn("provider", JavaDistribution::provider)
            .addAll(providers.stream().flatMap(provider -> provider.getDistributions().stream()).toList())
            .log(console::write);
    }

    private void listRuntimes(int version) {
        JavaDistributionProvider service = context.getService();
        JavaDistribution javaDistribution = context.getDistribution(service);
        List<JavaRuntime> runtimes = service.getRuntimes(javaDistribution, version, os, cpu);
        tableProvider.<JavaRuntime>get()
            .withColumn("distribution", JavaRuntime::distribution)
            .withColumn("version", runtime -> String.valueOf(runtime.version()))
            .withColumn("name", JavaRuntime::name)
            .withColumn("id", JavaRuntime::id)
            .addAll(runtimes)
            .log(console::write);
    }

}

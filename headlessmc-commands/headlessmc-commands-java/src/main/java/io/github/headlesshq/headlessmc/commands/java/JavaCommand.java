package io.github.headlesshq.headlessmc.commands.java;

import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.distribution.JavaDistribution;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProvider;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProviderService;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

@Getter
@Setter
@Default
@Dependent
@Named("command:java")
@CommandLine.Command(
    name = "java",
    mixinStandardHelpOptions = true,
    description = "Manage java installations.",
    subcommands = {
        InstallCommand.class,
        ListCommand.class,
        RemoveCommand.class
    }
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class JavaCommand implements CachedConsole.Enabled {
    private final JavaDistributionProviderService service;

    @CommandLine.Option(
        names = {"-p", "--provider"},
        description = "The provider to use. Default: " + JavaDistributionProvider.DEFAULT,
        completionCandidates = JavaProviderCompletions.class
    )
    private @Nullable String provider;

    @CommandLine.Option(
        names = {"-d", "--distribution"},
        description = "The java distribution to use. Default: " + JavaDistribution.DEFAULT
    )
    private @Nullable String distribution;

    // TODO: should this also run a command?

    JavaDistributionProvider getService() {
        if (provider == null) {
            return service.getDefaultProvider();
        }

        return service.getProviderByName(provider)
            .orElseThrow(() -> new IllegalArgumentException("Failed to find java distribution provider " + provider));
    }

    JavaDistribution getDistribution(JavaDistributionProvider service) {
        String distribution = this.distribution;
        if (distribution == null) {
            distribution = JavaDistribution.DEFAULT;
        }

        try {
            return service.getDistributionByName(distribution);
        } catch (HeadlessMcException e) {
            throw new IllegalArgumentException("Failed to find distribution " + distribution + " with " + service.getName(), e);
        }
    }

}

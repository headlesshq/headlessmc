package io.github.headlesshq.headlessmc.console.cdi;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.console.*;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.cache.CachedConsoleProvider;
import io.github.headlesshq.headlessmc.console.cache.ConsoleCachingService;
import io.github.headlesshq.headlessmc.console.input.ConsoleInputProvider;
import io.github.headlesshq.headlessmc.console.input.InputProvider;
import io.github.headlesshq.headlessmc.console.input.SystemInputProvider;
import io.github.headlesshq.headlessmc.console.jline.JlineConfig;
import io.github.headlesshq.headlessmc.console.jline.JlineConsoleProvider;
import io.github.headlesshq.headlessmc.console.output.FileDescriptorOutput;
import io.github.headlesshq.headlessmc.console.output.OutputProvider;
import io.github.headlesshq.headlessmc.console.output.StdOutOutput;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

@ApplicationScoped
public class ConsoleFactory {
    @Produces
    @Default
    @ApplicationScoped
    public Console getConsole(@Any Instance<ConsoleProvider> providers) {
        return new DefaultConsole(providers.stream().sorted().toList());
    }

    @Produces
    @Jline
    @ApplicationScoped
    public JlineConsoleProvider createJlineConsoleProvider(Holder<JlineConfig> configHolder) {
        return new JlineConsoleProvider(configHolder);
    }

    @Produces
    @Cache
    @ApplicationScoped
    public CachedConsoleProvider createConsoleCachingService(ConsoleCachingService service) {
        return new CachedConsoleProvider(service);
    }

    @Produces
    @Default
    @ApplicationScoped
    public ConsoleCachingService consoleCachingService() {
        return new ConsoleCachingService();
    }

    @Produces
    @Default
    @ApplicationScoped
    public ConsoleProvider getConsoleProviderImpl(
        @Any Instance<OutputProvider> outputProviders,
        @Any Instance<InputProvider> inputProviders
    ) {
        return SimpleConsoleProvider.of(
            outputProviders.stream().toList(),
            inputProviders.stream().toList()
        );
    }

    @Produces
    @ApplicationScoped
    @Named("io.github.headlesshq.headlessmc.console.input.ConsoleInputProvider")
    public InputProvider getConsoleInputProvider() {
        return new ConsoleInputProvider();
    }

    @Produces
    @ApplicationScoped
    @Named("io.github.headlesshq.headlessmc.console.input.SystemInputProvider")
    public InputProvider getSystemInputProvider() {
        return new SystemInputProvider();
    }

    @Produces
    @ApplicationScoped
    @Named("io.github.headlesshq.headlessmc.console.output.StdOutOutput")
    public OutputProvider getStdOutput() {
        return new StdOutOutput();
    }

    @Produces
    @ApplicationScoped
    @Named("io.github.headlesshq.headlessmc.console.output.FileDescriptorOutput")
    public OutputProvider getFileDescriptorOutput() {
        return new FileDescriptorOutput();
    }

}

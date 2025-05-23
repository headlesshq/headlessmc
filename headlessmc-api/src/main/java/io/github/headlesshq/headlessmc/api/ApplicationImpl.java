package io.github.headlesshq.headlessmc.api;

import io.github.headlesshq.headlessmc.api.command.CommandLineManager;
import io.github.headlesshq.headlessmc.api.di.Injector;
import io.github.headlesshq.headlessmc.api.exit.ExitManager;
import io.github.headlesshq.headlessmc.api.settings.Config;
import io.github.headlesshq.headlessmc.api.settings.SettingGroup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@ApplicationScoped
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class ApplicationImpl implements Application {
    private final Object lock = new Object();
    private final SettingGroup settings;
    private final Injector injector;
    private final ExitManager exitManager;
    private final CommandLineManager commandLine;
    private final Config config;

}

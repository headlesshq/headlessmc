package io.github.headlesshq.headlessmc.api;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import io.github.headlesshq.headlessmc.api.classloading.Deencapsulator;
import io.github.headlesshq.headlessmc.api.command.line.CommandLineManager;
import io.github.headlesshq.headlessmc.api.config.HasConfig;
import io.github.headlesshq.headlessmc.api.exit.ExitManager;
import io.github.headlesshq.headlessmc.logging.LoggingService;

/**
 * A default implementation for the {@link HeadlessMc} interface.
 */
@Getter
@RequiredArgsConstructor
public class HeadlessMcImpl implements HeadlessMc {
    private final Deencapsulator deencapsulator = new Deencapsulator();
    @Delegate
    @Getter(AccessLevel.NONE)
    private final HasConfig configHolder;
    private final CommandLineManager commandLine;
    private final ExitManager exitManager;
    private final LoggingService loggingService;

}

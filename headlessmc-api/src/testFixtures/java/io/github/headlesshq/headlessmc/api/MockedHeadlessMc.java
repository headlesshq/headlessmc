package io.github.headlesshq.headlessmc.api;

import lombok.Getter;
import lombok.Setter;
import io.github.headlesshq.headlessmc.api.classloading.Deencapsulator;
import io.github.headlesshq.headlessmc.api.command.line.CommandLineManager;
import io.github.headlesshq.headlessmc.api.config.Config;
import io.github.headlesshq.headlessmc.api.config.ConfigImpl;
import io.github.headlesshq.headlessmc.api.exit.ExitManager;
import io.github.headlesshq.headlessmc.logging.LoggingService;

@Getter
@Setter
public class MockedHeadlessMc implements HeadlessMc {
    // no enum cause @Setter
    public static final MockedHeadlessMc INSTANCE = new MockedHeadlessMc();
    private Deencapsulator deencapsulator = new Deencapsulator();
    private MockedExitManager exitManager = new MockedExitManager();
    private CommandLineManager commandLine = new CommandLineManager();
    private LoggingService loggingService = new LoggingService();
    private Config config = ConfigImpl.empty();
    private String log = null;

    @Override
    public void log(String message) {
        this.log = message;
    }

    public static class MockedExitManager extends ExitManager {
        public MockedExitManager() {
            setExitManager(i -> {});
        }
    }

}

package io.github.headlesshq.headlessmc.test;

import io.github.headlesshq.headlessmc.config.ConfigException;
import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Consumer;

import static io.github.headlesshq.headlessmc.test.CommandTest.SERVER_TEST_RESOURCE;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class CommandTestService {
    private final Holder<TestConfig> config;
    private final JsonService jsonService;

    public boolean isTestActive() {
        TestConfig config = this.config.get();
        if (config.server()) {
            if (config.file().isPresent()) {
                throw new ConfigException(
                    "Both hmc.test.server and hmc.test.file were specified, but only one can be applied"
                );
            }

            return true;
        }

        return config.file().isPresent();
    }

    public @Nullable CommandTest createCommandTest(Process process, Consumer<String> output) throws IOException {
        TestConfig config = this.config.get();

        TestCase test;
        if (config.server()) {
            if (config.file().isPresent()) {
                throw new ConfigException(
                    "Both hmc.test.server and hmc.test.file were specified, but only one can be applied"
                );
            }

            try (InputStream is = CommandTest.class.getClassLoader().getResourceAsStream(SERVER_TEST_RESOURCE)) {
                test = jsonService.parse(is, TestCase.class);
            }
        } else {
            String fileName = config.file().orElse(null);
            if (fileName == null) {
                return null;
            }

            try (InputStream is = Files.newInputStream(Paths.get(fileName))) {
                test = jsonService.parse(is, TestCase.class);
            }
        }

        return new CommandTest(output, test, process, config.noTimeout());
    }

}

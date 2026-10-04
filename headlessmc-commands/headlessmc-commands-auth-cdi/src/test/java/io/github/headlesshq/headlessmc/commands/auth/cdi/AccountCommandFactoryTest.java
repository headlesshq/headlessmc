package io.github.headlesshq.headlessmc.commands.auth.cdi;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

@QuarkusTest
public class AccountCommandFactoryTest {
    @Inject
    public CommandLine commandLine;

    @Test
    public void testAccountCommand() {
        commandLine.execute("account", "list");
        commandLine.execute("account", "refresh");
    }

}

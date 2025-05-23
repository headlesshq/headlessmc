package io.github.headlesshq.headlessmc.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.github.headlesshq.headlessmc.api.Application;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

public class GuiceTest {
    @Test
    public void test() {
        Injector injector = Guice.createInjector(new ApplicationModule());
        Application application = injector.getInstance(Application.class);
        assertInstanceOf(GuiceInjector.class, application.getInjector());
        application.getCommandLine().getContext().getPicocli().addSubcommand(TestCommand.class);
        application.getCommandLine().getContext().execute("test");
        Application injected = application.getCommandLine().getContext().getPicocli().getSubcommands().get("test").getExecutionResult();
        assertSame(application, injected);
    }

    @CommandLine.Command(
            name = "test"
    )
    @RequiredArgsConstructor(onConstructor = @__(@Inject))
    private static class TestCommand implements Callable<Application> {
        private final Application application;

        @Override
        public Application call() {
            return application;
        }
    }

}

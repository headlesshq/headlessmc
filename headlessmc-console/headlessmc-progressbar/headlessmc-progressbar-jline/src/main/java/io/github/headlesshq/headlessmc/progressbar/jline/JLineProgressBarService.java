package io.github.headlesshq.headlessmc.progressbar.jline;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;
import io.github.headlesshq.headlessmc.progressbar.ProgressBar;
import io.github.headlesshq.headlessmc.progressbar.ProgressbarService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import me.tongfei.progressbar.ConsoleProgressBarConsumer;
import me.tongfei.progressbar.InteractiveConsoleProgressBarConsumer;
import me.tongfei.progressbar.ProgressBarBuilder;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Optional;

@ApplicationScoped
public class JLineProgressBarService implements ProgressbarService {
    private final Instance<Console> console;

    public JLineProgressBarService(Instance<Console> console) {
        this.console = console;
    }

    @Override
    public String getName() {
        return "jline";
    }

    // TODO: config
    @Override
    public ProgressBar displayProgressBar(ProgressBar.Configuration configuration) {
        try {
            ProgressBarBuilder builder = new ProgressBarBuilder()
                .setTaskName(configuration.taskName())
                .showSpeed() // TODO
                .setInitialMax(configuration.initialMax());

            if (configuration.unit() != null) {
                builder.setUnit(configuration.unit().name(), configuration.unit().size());
            }

            createConsumer().ifPresent(builder::setConsumer);
            return new JlineProgressBar(builder.build());
        } catch (UncheckedInterruptedException e) {
            throw e;
        } catch (Exception e) {
            //noinspection ConstantValue
            if (e instanceof InterruptedException interruptedException) {
                throw new UncheckedInterruptedException(interruptedException);
            }

            throw new HeadlessMcIOException("Failed to create progressbar with service " + getName(), e);
        }
    }

    /**
     * Without a consumer, ctongfei/progressbar opens its own Jline system terminal
     * to detect the terminal width and cursor movement support.
     * Jline only allows one system terminal at a time, so while our console has a terminal open,
     * the progressbar would only get a dumb terminal without a width.
     * Instead, we ask our console for these properties.
     * If there is no console or it is not a Jline console, nothing conflicts and the progressbar can detect them itself.
     */
    private Optional<ConsoleProgressBarConsumer> createConsumer() {
        if (!console.isResolvable()) {
            return Optional.empty();
        }

        return console.get().extensions().map(extensions -> {
            // same as ctongfei/progressbar, System.err might be overridden by System.setErr
            PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.err));
            // ctongfei/progressbar also leaves the last column free
            int width = extensions.getWidth() - 1;
            return extensions.supportsCursorMovement()
                ? new InteractiveConsoleProgressBarConsumer(out, width)
                : new ConsoleProgressBarConsumer(out, width);
        });
    }

}

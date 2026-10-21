package io.github.headlesshq.headlessmc.progressbar.jline;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.console.jline.JlineConfig;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;
import io.github.headlesshq.headlessmc.progressbar.ProgressBar;
import io.github.headlesshq.headlessmc.progressbar.ProgressbarService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import me.tongfei.progressbar.ProgressBarBuilder;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class JLineProgressBarService implements ProgressbarService {
    private final Holder<JlineConfig> config;

    @Override
    public String getName() {
        return "jline";
    }

    /**
     * The progressbar opens its own Jline terminal, which may start processes (stty) to inspect the terminal,
     * so it is only used if Jline is enabled ({@code hmc.jline.enabled}).
     */
    @Override
    public boolean isDummy() {
        return !config.get().enabled();
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

}

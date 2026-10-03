package io.github.headlesshq.headlessmc.progressbar.jline;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;
import io.github.headlesshq.headlessmc.progressbar.ProgressBar;
import io.github.headlesshq.headlessmc.progressbar.ProgressbarService;
import jakarta.enterprise.context.ApplicationScoped;
import me.tongfei.progressbar.ProgressBarBuilder;

@ApplicationScoped
public class JLineProgressBarService implements ProgressbarService {
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

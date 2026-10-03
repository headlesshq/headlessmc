package io.github.headlesshq.headlessmc.console.cdi;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.console.format.SimpleTable;
import io.github.headlesshq.headlessmc.console.format.TableBuilder;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.console.jline.AnsiTable;
import io.github.headlesshq.headlessmc.console.jline.JlineConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class TableProviderFactory {
    @Produces
    @Default
    @Dependent
    public TableProvider getTableProvider(Holder<JlineConfig> configHolder) {
        return new TableProvider() {
            @Override
            public <T> TableBuilder<T> get() {
                JlineConfig config = configHolder.get();
                if (config.enabled() || config.ansiColors()) {
                    return new AnsiTable<>();
                }

                return new SimpleTable<>();
            }
        };
    }

}

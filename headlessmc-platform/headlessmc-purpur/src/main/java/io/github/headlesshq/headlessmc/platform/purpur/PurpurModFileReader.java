package io.github.headlesshq.headlessmc.platform.purpur;

import io.github.headlesshq.headlessmc.platform.paper.PaperModFileReader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Purpur
@ApplicationScoped
public class PurpurModFileReader extends PaperModFileReader {
    @Inject
    public PurpurModFileReader() {
        this("plugin.yml", "paper-plugin.yml");
    }

    public PurpurModFileReader(String... ymls) {
        super(ymls);
    }

}

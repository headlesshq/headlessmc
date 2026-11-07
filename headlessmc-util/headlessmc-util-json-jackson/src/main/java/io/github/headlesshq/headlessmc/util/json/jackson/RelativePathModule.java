package io.github.headlesshq.headlessmc.util.json.jackson;

import io.github.headlesshq.headlessmc.util.json.RelativePathService;
import tools.jackson.databind.module.SimpleModule;

import java.nio.file.Path;

/**
 * Registers the {@link RelativePathSerializer} and {@link RelativePathDeserializer} for {@link Path}.
 */
public class RelativePathModule extends SimpleModule {
    public RelativePathModule(RelativePathService relativePathService) {
        super(RelativePathModule.class.getSimpleName());
        addSerializer(Path.class, new RelativePathSerializer(relativePathService));
        addDeserializer(Path.class, new RelativePathDeserializer(relativePathService));
    }

}

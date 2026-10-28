package io.github.headlesshq.headlessmc.util.json.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.util.json.Lenient;
import io.github.headlesshq.headlessmc.util.json.RelativePathService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Accepts line breaks in strings.
 * Needed to read fabric mod JSONs.
 */
@Lenient
@ApplicationScoped
public class LenientJacksonJsonService extends AbstractJacksonJsonService implements JsonService {
    private final ObjectMapper mapper;

    @Inject
    public LenientJacksonJsonService(RelativePathService relativePathService) {
        this.mapper = JsonMapper.builder()
            .changeDefaultVisibility( // do not serialize getters
                visibility -> visibility
                    .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                    .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                //.withCreatorVisibility(JsonAutoDetect.Visibility.NONE) ?
            ).configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .addModule(new RelativePathModule(relativePathService))
            .build();
    }

    /**
     * Creates a service without any {@link io.github.headlesshq.headlessmc.util.json.RelativePathSource}s,
     * absolute paths are written as is.
     */
    public LenientJacksonJsonService() {
        this(new RelativePathService(List.of()));
    }

    @Override
    protected ObjectMapper mapper() {
        return mapper;
    }

}

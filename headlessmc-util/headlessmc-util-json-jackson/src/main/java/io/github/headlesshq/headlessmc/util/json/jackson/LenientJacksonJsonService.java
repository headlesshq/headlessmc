package io.github.headlesshq.headlessmc.util.json.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.util.json.Lenient;
import jakarta.enterprise.context.ApplicationScoped;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Accepts line breaks in strings.
 * Needed to read fabric mod JSONs.
 */
@Lenient
@ApplicationScoped
public class LenientJacksonJsonService extends AbstractJacksonJsonService implements JsonService {
    protected ObjectMapper mapper() {
        return JsonMapper.builder()
            .changeDefaultVisibility( // do not serialize getters
                visibility -> visibility
                    .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                    .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                //.withCreatorVisibility(JsonAutoDetect.Visibility.NONE) ?
            ).configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .build();
    }

}

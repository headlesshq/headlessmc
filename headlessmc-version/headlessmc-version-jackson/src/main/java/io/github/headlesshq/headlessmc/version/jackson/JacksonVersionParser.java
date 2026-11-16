package io.github.headlesshq.headlessmc.version.jackson;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.ParsingException;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.Reader;
import java.nio.file.Path;

@ApplicationScoped
@RequiredArgsConstructor
public class JacksonVersionParser implements VersionParser {
    private final ObjectMapper mapper;

    @Inject
    public JacksonVersionParser() {
        // For parsing JacksonVersion.ArgumentsImpl.ArgumentImpl.value
        this(JsonMapper.builder().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY).build());
    }

    @Override
    public Version parse(Reader reader) throws HeadlessMcException {
        try {
            return mapper.readValue(reader, JacksonVersion.class);
        } catch (JacksonException e) {
            throw new ParsingException(e);
        }
    }

    /**
     * Parses the given version.json file and connects the parsed {@link Version} to it,
     * see {@link Version#asFile()}.
     *
     * @param jsonFile the Minecraft version.json file to parse.
     * @return the parsed Version, whose {@link Version#asFile()} is the given file.
     * @throws HeadlessMcException if the file does not exist, cannot be read, or parsed.
     */
    @Override
    public Version parse(Path jsonFile) throws HeadlessMcException {
        Version version = VersionParser.super.parse(jsonFile);
        if (version instanceof JacksonVersion jacksonVersion) {
            jacksonVersion.setFile(jsonFile.toAbsolutePath().normalize());
        }

        return version;
    }

    Version parse(JsonNode node) throws HeadlessMcException {
        try {
            return mapper.treeToValue(node, JacksonVersion.class);
        } catch (JacksonException e) {
            throw new ParsingException(e);
        }
    }

    String format(JsonNode node) {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    }

}

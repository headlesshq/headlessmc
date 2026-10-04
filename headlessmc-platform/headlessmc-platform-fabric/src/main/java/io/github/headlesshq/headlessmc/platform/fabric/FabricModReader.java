package io.github.headlesshq.headlessmc.platform.fabric;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.ParsingException;
import io.github.headlesshq.headlessmc.platform.AbstractJarEntryModReader;
import io.github.headlesshq.headlessmc.platform.mods.Mod;
import io.github.headlesshq.headlessmc.platform.mods.ModReader;
import io.github.headlesshq.headlessmc.util.json.JsonParseException;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.Lenient;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link ModReader} for {@code fabric.mod.json} files.
 *
 * @see <a href=https://wiki.fabricmc.net/documentation:fabric_mod_json>
 * https://wiki.fabricmc.net/documentation:fabric_mod_json
 * </a>
 * @see <a href=https://github.com/FabricMC/fabric-loader/blob/35b0b1c0268eb5f9d377322db491b0bb436541a8/src/main/java/net/fabricmc/loader/impl/metadata/V0ModMetadataParser.java>
 * V0ModMetadataParser
 * </a>
 * @see <a href=https://github.com/FabricMC/fabric-loader/blob/35b0b1c0268eb5f9d377322db491b0bb436541a8/src/main/java/net/fabricmc/loader/impl/metadata/V1ModMetadataParser.java>
 * V1ModMetadataParser
 * </a>
 */
@Fabric
@ApplicationScoped
public class FabricModReader extends AbstractJarEntryModReader {
    // TODO: I really dislike Field injection...
    @Inject
    @Setter
    @Lenient
    JsonService jsonService;

    /**
     * Constructs the default @Fabric instance to inject.
     */
    @Inject
    FabricModReader() {
        super("fabric.mod.json");
    }

    /**
     * Constructs a new FabricModReader for the specified arguments.
     *
     * @param entries the names of the entries within a jar to check.
     */
    @SuppressWarnings("unused")
    public FabricModReader(JsonService service, String... entries) {
        super(entries);
        this.jsonService = service;
    }

    @Override
    public Optional<List<Mod>> readEntry(InputStream inputStream) throws HeadlessMcException {
        /*
        TODO: need to be even more lenient, this is allowed:
          "schemaVersion": 1,
  "id": "entity_model_features",
  "version": "3.2.6",
  "name": "Entity Model Features",
  // TODO: here the description string contains a line break!
  "description": "This is an expansion of the ETF mod, it adds support for OptiFine format Custom Entity Model (CEM) resource packs.
While still allowing you to disable this to use a different model mod :)",
  "authors": [
         */
        try {
            @SuppressWarnings("Convert2Diamond")
            List<FabricModJson> jsonMods = jsonService.parseList(inputStream, new TypeLiteral<List<FabricModJson>>() {});
            return Optional.of(jsonMods.stream().map(FabricModJson::toMod).toList());
        } catch (JsonParseException e) {
            throw new ParsingException(e);
        }
    }

    @RegisterForReflection
    private record FabricModJson(
        String id,
        @Nullable String name,
        @Nullable String description,
        @JsonDeserialize(contentUsing = AuthorDeserializer.class)
        //@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        @Nullable List<String> authors,
        //@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        @Nullable Map<String, List<String>> depends,
        //@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        @Nullable Map<String, List<String>> requires
    ) implements ReflectionRegistered {
        public Mod toMod() {
            Objects.requireNonNull(id, "Mod id was null:" + this);
            return new Mod(
                id,
                name == null ? id : name,
                Optional.ofNullable(description),
                authors == null ? List.of() : authors,
                depends == null ? (requires == null ? Map.of() : requires) : depends
            );
        }

        @RegisterForReflection
        private record AuthorObject(@Nullable String name) { }

        @RegisterForReflection
        private static final class AuthorDeserializer extends StdDeserializer<String> {
            AuthorDeserializer() {
                super(String.class);
            }

            @Override
            public String deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
                JsonToken token = p.currentToken();
                if (JsonToken.VALUE_STRING.equals(token)) {
                    return p.getString();
                }

                if (JsonToken.START_OBJECT.equals(token)) {
                    // TODO: could be simpler by just reading ObjectNode and getting "name" key...
                    String author = ctxt.readValue(p, AuthorObject.class).name();
                    return author == null ? "" : author;
                }

                return (String) ctxt.handleUnexpectedToken(String.class, p);
            }
        }
    }

}

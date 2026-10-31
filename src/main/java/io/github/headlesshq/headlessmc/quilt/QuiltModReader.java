package io.github.headlesshq.headlessmc.quilt;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.ParsingException;
import io.github.headlesshq.headlessmc.platform.AbstractJarEntryModReader;
import io.github.headlesshq.headlessmc.platform.mods.Mod;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.JsonParseException;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.util.json.Lenient;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code ModReader} for {@code quilt.mod.json} files.
 * Fabric mods, which Quilt can load as well, are read by the FabricModReader of HeadlessMc.
 * A {@link Singleton}, because normal scoped beans need a no-args constructor to be proxied,
 * which {@link AbstractJarEntryModReader} does not have.
 *
 * @see <a href=https://github.com/QuiltMC/rfcs/blob/main/specification/0002-quilt.mod.json.md>quilt.mod.json specification</a>
 */
@Quilt
@Singleton
public class QuiltModReader extends AbstractJarEntryModReader {
    public static final String QUILT_MOD_JSON = "quilt.mod.json";

    private final JsonService jsonService;

    @Inject
    public QuiltModReader(@Lenient JsonService jsonService) {
        super(QUILT_MOD_JSON);
        this.jsonService = jsonService;
    }

    @Override
    public Optional<List<Mod>> readEntry(InputStream inputStream) throws HeadlessMcException {
        try {
            QuiltModJson json = jsonService.parse(inputStream, QuiltModJson.class);
            if (json.quilt_loader() == null || json.quilt_loader().id() == null) {
                return Optional.empty();
            }

            return Optional.of(List.of(json.quilt_loader().toMod()));
        } catch (JsonParseException e) {
            throw new ParsingException(e);
        }
    }

    @RegisterForReflection
    record QuiltModJson(int schema_version, @Nullable QuiltLoader quilt_loader) implements ReflectionRegistered {

    }

    @RegisterForReflection
    record QuiltLoader(
        String id,
        @Nullable String version,
        @Nullable Metadata metadata,
        @Nullable List<JsonNode> depends
    ) implements ReflectionRegistered {
        Mod toMod() {
            Metadata metadata = this.metadata;
            String name = metadata == null || metadata.name() == null ? id : metadata.name();
            Optional<String> description = metadata == null ? Optional.empty() : Optional.ofNullable(metadata.description());
            List<String> authors = metadata == null || metadata.contributors() == null
                ? List.of()
                : List.copyOf(metadata.contributors().keySet());
            Map<String, List<String>> dependencies = new LinkedHashMap<>();
            if (depends != null) {
                for (JsonNode dependency : depends) {
                    addDependency(dependency, dependencies);
                }
            }

            return new Mod(id, name, description, authors, dependencies);
        }

        /**
         * A dependency is either a mod id, an object with an id and versions,
         * or an array of those, of which any one has to be present.
         */
        private static void addDependency(JsonNode dependency, Map<String, List<String>> dependencies) {
            if (dependency.isString()) {
                dependencies.putIfAbsent(dependency.asString(), List.of("*"));
            } else if (dependency.isArray()) {
                for (JsonNode alternative : dependency) {
                    addDependency(alternative, dependencies);
                }
            } else if (dependency.isObject() && dependency.has("id")) {
                JsonNode optional = dependency.get("optional");
                if (optional != null && optional.asBoolean(false)) {
                    return;
                }

                dependencies.put(dependency.get("id").asString(), versions(dependency.get("versions")));
            }
        }

        private static List<String> versions(@Nullable JsonNode versions) {
            if (versions == null || versions.isNull()) {
                return List.of("*");
            }

            if (versions.isArray()) {
                List<String> result = new ArrayList<>();
                versions.forEach(version -> result.add(version.asString()));
                return result;
            }

            // a string, or an object combining constraints with any/all
            return List.of(versions.isString() ? versions.asString() : versions.toString());
        }
    }

    @RegisterForReflection
    record Metadata(
        @Nullable String name,
        @Nullable String description,
        @Nullable Map<String, JsonNode> contributors
    ) implements ReflectionRegistered {

    }

}

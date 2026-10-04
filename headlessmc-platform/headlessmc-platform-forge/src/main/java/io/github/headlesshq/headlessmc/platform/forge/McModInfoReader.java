package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.ParsingException;
import io.github.headlesshq.headlessmc.platform.AbstractJarEntryModReader;
import io.github.headlesshq.headlessmc.platform.mods.Mod;
import io.github.headlesshq.headlessmc.platform.mods.ModReader;
import io.github.headlesshq.headlessmc.util.json.JsonParseException;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link ModReader} for legacy mcmod.info files.
 *
 * @see <a href=https://docs.minecraftforge.net/en/1.12.x/gettingstarted/structuring/>
 * https://docs.minecraftforge.net/en/1.12.x/gettingstarted/structuring/
 * </a>
 */
@Forge
@Priority(200) // TODO: Instance.stream returns this before Priority(100) TomlReader...
@ApplicationScoped
public class McModInfoReader extends AbstractJarEntryModReader implements ModReader {
    // TODO: I really dislike Field injection...
    @Inject
    @Setter
    JsonService jsonService;

    @Inject
    McModInfoReader() {
        super("mcmod.info");
    }

    /**
     * Constructs an McModInfoReader for the given arguments.
     *
     * @param entries the entries to scan a jar for.
     */
    @SuppressWarnings("unused")
    public McModInfoReader(JsonService jsonService, String... entries) {
        super(entries);
        this.jsonService = jsonService;
    }

    @Override
    public Optional<List<Mod>> readEntry(InputStream inputStream) throws HeadlessMcException {
        try {
            return Optional.of(
                jsonService.parseList(inputStream, new TypeLiteral<List<McModInfo>>() {})
                    .stream()
                    .map(modInfo -> new Mod(
                        modInfo.modid,
                        modInfo.name,
                        Optional.ofNullable(modInfo.description),
                        modInfo.authorList == null ? List.of() : modInfo.authorList,
                        modInfo.getDependencies()
                    )).toList()
            );
        } catch (JsonParseException e) {
            throw new ParsingException(e);
        }
    }

    @RegisterForReflection
    private record McModInfo(
        String modid,
        String name,
        @Nullable String url,
        @Nullable String mcVersion,
        @Nullable String logoFile,
        @Nullable String description,
        @Nullable List<String> authorList,
        @Nullable List<String> requiredMods,
        @Nullable Boolean useDependencyInformation
    ) implements ReflectionRegistered {
        Map<String, List<String>> getDependencies() {
            Map<String, List<String>> result = new HashMap<>();
            if (requiredMods == null || useDependencyInformation == null || !useDependencyInformation) {
                return result;
            }

            requiredMods.forEach(modId -> result.put(modId, List.of()));
            return result;
        }
    }

}

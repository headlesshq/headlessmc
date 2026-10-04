package io.github.headlesshq.headlessmc.platform.paper;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.ParsingException;
import io.github.headlesshq.headlessmc.platform.AbstractJarEntryModReader;
import io.github.headlesshq.headlessmc.platform.mods.Mod;
import io.github.headlesshq.headlessmc.platform.mods.ModReader;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Parses the Paper plugin meta files.
 *
 * @see <a href=https://github.com/PaperMC/Paper/blob/76d2ac758cb3abe75aceefa88207443768f585c6/paper-api/src/main/java/io/papermc/paper/plugin/configuration/PluginMeta.java>
 * PluginMeta
 * </a>
 * @see <a href=https://github.com/PaperMC/Paper/blob/76d2ac758cb3abe75aceefa88207443768f585c6/paper-server/src/main/java/io/papermc/paper/plugin/provider/configuration/PaperPluginMeta.java>
 * PluginMeta Implementation
 * </a>
 */
@Paper
@ApplicationScoped
public class PaperModReader extends AbstractJarEntryModReader implements ModReader {
    private final ObjectMapper mapper;

    public PaperModReader(ObjectMapper mapper, String... entryNames) {
        super(entryNames);
        this.mapper = mapper;
    }

    @Inject
    public PaperModReader() {
        this(new YAMLMapper(), "plugin.yml", "paper-plugin.yml");
    }

    @Override
    public Optional<List<Mod>> readEntry(InputStream inputStream) throws HeadlessMcException {
        try {
            PluginMeta meta = mapper.readValue(inputStream, PluginMeta.class);
            return Optional.of(List.of(
                new Mod(
                    meta.name,
                    meta.name,
                    Optional.ofNullable(meta.description),
                    meta.getAuthors(),
                    Map.of()
                )
            ));
        } catch (JacksonException e) {
            throw new ParsingException(e);
        }
    }

    @RegisterForReflection
    public record PluginMeta(
        String name,
        // String main,
        //@Nullable List<String> provides,
        String version,
        @Nullable String description,
        @Nullable List<String> authors,
        //@Nullable List<String> contributors,
        //@Nullable String website,
        //@Nullable String prefix,
        //@Nullable String apiVersion,
        @Nullable String author // not sure if this exists? or is handled?
    ) {
        public List<String> getAuthors() {
            String author = this.author;
            List<String> authors = this.authors;
            List<String> result = new ArrayList<>();
            if (authors != null) {
                result.addAll(authors);
            }

            if (author != null && !result.contains(author)) {
                result.add(author);
            }

            return result;
        }
    }

}

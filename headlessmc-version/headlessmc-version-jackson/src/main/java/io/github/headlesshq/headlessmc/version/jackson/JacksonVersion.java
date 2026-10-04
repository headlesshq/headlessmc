package io.github.headlesshq.headlessmc.version.jackson;

import io.github.headlesshq.headlessmc.version.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.type.TypeFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@RegisterForReflection
@SuppressWarnings("ClassCanBeRecord") // TODO: maybe?
final class JacksonVersion implements Version {
    private final String id;
    private final @Nullable String inheritsFrom;
    private final @Nullable String type;
    @JsonDeserialize(contentAs = LibraryImpl.class)
    private final @Nullable List<Library> libraries;
    private final @Nullable String mainClass;
    private final @Nullable String minecraftArguments;
    @JsonDeserialize(contentUsing = ArgumentListDeserializer.class)
    private final @Nullable Map<String, List<ArgumentImpl>> arguments;
    private final @Nullable AssetIndexImpl assetIndex;
    @JsonDeserialize(contentAs = LoggingConfigurationImpl.class)
    private final @Nullable Map<String, LoggingConfiguration> logging;
    @JsonDeserialize(contentAs = DownloadImpl.class)
    private final @Nullable Map<String, Download> downloads;
    private final @Nullable JavaVersionImpl javaVersion;

    @Override
    public List<Library> getLibraries() {
        return libraries == null ? List.of() : libraries;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"}) // this is safe
    public @Nullable Map<String, List<Argument>> getArguments() {
        return (Map) arguments;
    }

    @Data
    @RegisterForReflection
    static final class LibraryImpl implements Library {
        private final @Nullable Map<String, String> natives;
        private final String name;
        private final @Nullable ExtractImpl extract;
        @JsonDeserialize(contentAs = RuleImpl.class)
        private final @Nullable List<Rule> rules;
        private final @Nullable LibraryDownloadsImpl downloads;
        private final @Nullable String md5;
        private final @Nullable String sha1;
        private final @Nullable String sha256;
        private final @Nullable String sha512;
        private final @Nullable Long size;
        private final @Nullable String url;
        private final @Nullable String path;

        @Override // libraries do not have an id, this is a convenience abstraction of Download
        public String getId() {
            return getName();
        }

        @Data
        @NoArgsConstructor
        @RegisterForReflection
        static final class ExtractImpl implements Extract {
            private List<String> exclude = new ArrayList<>();
        }

        @Data
        @RegisterForReflection
        static final class LibraryDownloadsImpl implements LibraryDownloads {
            @JsonDeserialize(contentAs = DownloadImpl.class)
            private final @Nullable Map<String, Download> classifiers;
            private final @Nullable DownloadImpl artifact;
        }
    }

    @Data
    @RegisterForReflection
    static final class DownloadImpl implements Download {
        private final @Nullable String id;
        private final @Nullable String sha1;
        private final @Nullable Long size;
        private final @Nullable String url;
        private final @Nullable String path;
    }

    @Data
    @RegisterForReflection
    static final class RuleImpl implements Version.Rule {
        private final String action;
        private final @Nullable OSImpl os;
        private final @Nullable Map<String, Boolean> features;
    }

    @Data
    @RegisterForReflection
    static final class OSImpl implements Version.Rule.OS {
        private final @Nullable String name;
        private final @Nullable String version;
        private final @Nullable String arch;
        private final @Nullable VersionRangeImpl versionRange;
    }

    @Data
    @RegisterForReflection
    static final class VersionRangeImpl implements Rule.OS.VersionRange {
        private final @Nullable String min;
        private final @Nullable String max;
    }

    @Data
    @RegisterForReflection
    static final class AssetIndexImpl implements AssetIndex {
        private final String id;
        private final Long totalSize;
        private final @Nullable String sha1;
        private final @Nullable Long size;
        private final @Nullable String url;
        private final @Nullable String path;
    }

    @Data
    @RegisterForReflection
    static final class LoggingConfigurationImpl implements LoggingConfiguration {
        private final String argument;
        private final DownloadImpl file;
        private final String type;
    }

    @Data
    @RegisterForReflection
    static final class JavaVersionImpl implements JavaVersion {
        private final String component;
        private final Integer majorVersion;
    }

    @Data
    @RegisterForReflection
    static final class ArgumentImpl implements Version.Argument {
        @JsonDeserialize(contentAs = RuleImpl.class)
        private final @Nullable List<Rule> rules;
        // jackson 2: @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        // requires Object mapper to be configured correctly
        private final List<String> value;

        @Override
        public List<String> value() {
            return value;
        }
    }

    @RegisterForReflection
    static final class ArgumentListDeserializer extends StdDeserializer<List<Argument>> {
        private final ArgumentDeserializer argumentDeserializer = new ArgumentDeserializer();

        ArgumentListDeserializer() {
            super(new TypeFactory().constructType(new TypeReference<List<Argument>>() {}));
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<Argument> deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            if (!p.isExpectedStartArrayToken()) {
                return (List<Argument>) ctxt.handleUnexpectedToken(this.getValueType(), p);
            }

            List<Argument> result = new ArrayList<>();
            while (p.nextToken() != JsonToken.END_ARRAY) {
                result.add(argumentDeserializer.deserialize(p, ctxt));
            }

            return result;
        }
    }

    @RegisterForReflection
    static final class ArgumentDeserializer extends StdDeserializer<Argument> {
        ArgumentDeserializer() {
            super(Argument.class);
        }

        @Override
        public Argument deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            JsonToken token = p.currentToken();
            if (JsonToken.VALUE_STRING.equals(token)) {
                return new ArgumentImpl(null, List.of(p.getString()));
            }

            if (JsonToken.START_OBJECT.equals(token)) {
                return ctxt.readValue(p, ArgumentImpl.class);
            }

            return (Argument) ctxt.handleUnexpectedToken(ArgumentImpl.class, p);
        }
    }

}

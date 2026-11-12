package io.github.headlesshq.headlessmc.plugins;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * The index of officially supported plugins, published by
 * <a href="https://github.com/headlesshq/headlessmc-plugins">headlessmc-plugins</a>, see {@link PluginIndexService}.
 * Unknown fields are ignored, so that the format can be extended.
 *
 * @param schemaVersion the version of this format, currently {@value #SCHEMA_VERSION}.
 * @param plugins       the plugins in the index.
 */
@RegisterForReflection
public record PluginIndex(int schemaVersion, @Nullable List<Entry> plugins) implements ReflectionRegistered {
    /**
     * The supported {@link #schemaVersion()}.
     */
    public static final int SCHEMA_VERSION = 1;

    /**
     * @return {@link #plugins()} without {@code null}s, or an empty list.
     */
    public List<Entry> pluginList() {
        return Objects.requireNonNullElse(plugins, List.<Entry>of()).stream().filter(Objects::nonNull).toList();
    }

    /**
     * @param id the id of the plugin.
     * @return the plugin with the given id.
     */
    public Optional<Entry> find(String id) {
        return pluginList().stream().filter(entry -> id.equals(entry.id())).findFirst();
    }

    /**
     * A plugin in the index.
     *
     * @param id          the id of the plugin, the same as the id in its {@link PluginManifest}.
     * @param name        a human-readable name.
     * @param description a short description.
     * @param authors     the authors.
     * @param license     the license, preferably an SPDX identifier.
     * @param homepage    a link to the homepage or source code.
     * @param icon        a link to an icon.
     * @param downloads   the released versions of the plugin.
     */
    @RegisterForReflection
    public record Entry(
        String id,
        @Nullable String name,
        @Nullable String description,
        @Nullable List<String> authors,
        @Nullable String license,
        @Nullable String homepage,
        @Nullable String icon,
        @Nullable List<Release> downloads
    ) implements ReflectionRegistered {
        /**
         * @return {@link #name()}, or {@link #id()} if no name has been given.
         */
        public String displayName() {
            return Objects.requireNonNullElse(name, id);
        }

        /**
         * @return {@link #authors()} without {@code null}s, or an empty list.
         */
        public List<String> authorList() {
            return Objects.requireNonNullElse(authors, List.<String>of()).stream().filter(Objects::nonNull).toList();
        }

        /**
         * @return {@link #downloads()} without {@code null}s, or an empty list.
         */
        public List<Release> releaseList() {
            return Objects.requireNonNullElse(downloads, List.<Release>of()).stream().filter(Objects::nonNull).toList();
        }

        /**
         * @param version the version of the plugin.
         * @return the release with exactly the given version.
         */
        public Optional<Release> release(String version) {
            return releaseList().stream().filter(release -> version.equals(release.version())).findFirst();
        }

        /**
         * @param words the words to search for.
         * @return {@code true} if the id, name, description or authors contain all words, ignoring case.
         */
        public boolean matches(List<String> words) {
            String text = String.join("\n", id, Objects.requireNonNullElse(name, ""),
                Objects.requireNonNullElse(description, ""), String.join("\n", authorList())).toLowerCase(Locale.ROOT);
            return words.stream().allMatch(word -> text.contains(word.toLowerCase(Locale.ROOT)));
        }
    }

    /**
     * A released version of a plugin.
     *
     * @param version    the version of the plugin.
     * @param headlessmc the {@link io.github.headlesshq.headlessmc.plugins.version.VersionRange}
     *                   of HeadlessMc versions this version is compatible with.
     * @param java       the minimum Java version this version needs.
     * @param url        where to download the plugin jar from.
     * @param sha256     the SHA-256 hash of the plugin jar.
     * @param size       the size of the plugin jar in bytes.
     */
    @RegisterForReflection
    public record Release(
        String version,
        @Nullable String headlessmc,
        @Nullable Integer java,
        @Nullable String url,
        @Nullable String sha256,
        @Nullable Long size
    ) implements ReflectionRegistered {

    }

}

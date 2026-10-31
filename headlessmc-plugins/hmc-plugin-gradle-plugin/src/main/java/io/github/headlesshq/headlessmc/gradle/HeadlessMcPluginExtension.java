package io.github.headlesshq.headlessmc.gradle;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

/**
 * The {@code headlessmcPlugin} extension, describes the plugin.
 * Everything here ends up in {@code META-INF/headlessmc-plugin.json}, the version is the project version.
 */
public abstract class HeadlessMcPluginExtension {
    /**
     * @return the HeadlessMc version to compile against, e.g. 3.0.0-RC9. Defaults to the version of this Gradle plugin.
     */
    public abstract Property<String> getHeadlessmcVersion();

    /**
     * @return the unique id of the plugin: lowercase letters, digits, - and _.
     */
    public abstract Property<String> getId();

    /**
     * @return a human-readable name, defaults to the id.
     */
    public abstract Property<String> getName();

    /**
     * @return a short description, shown by {@code headlessmc plugin ls}.
     */
    public abstract Property<String> getDescription();

    public abstract ListProperty<String> getAuthors();

    /**
     * @return the license, preferably an SPDX identifier like MIT.
     */
    public abstract Property<String> getLicense();

    /**
     * @return a link to the homepage or the source code.
     */
    public abstract Property<String> getHomepage();

    /**
     * @return the range of HeadlessMc versions the plugin is compatible with, Maven range syntax, e.g. [3.0.0-RC9,4).
     * A bare version means up to the next major version. Defaults to {@link #getHeadlessmcVersion()}.
     */
    public abstract Property<String> getHeadlessmc();

    /**
     * @return the minimum Java version the plugin needs, at least and by default 25.
     */
    public abstract Property<Integer> getJava();

    /**
     * @return plugin id to the range of versions of the other plugins this plugin depends on.
     */
    public abstract MapProperty<String, String> getDependencies();

    /**
     * @return the ids of the {@link #getDependencies()} which do not need to be installed.
     */
    public abstract SetProperty<String> getOptionalDependencies();

    /**
     * @return group:artifact to the range of versions of an included library this plugin is compatible with.
     * Libraries not listed here default to [version, nextMajor).
     */
    public abstract MapProperty<String, String> getLibraryVersions();

    /**
     * Declares a required dependency on another plugin.
     *
     * @param id       the id of the other plugin.
     * @param versions the range of versions of the other plugin, e.g. [1.0,2).
     */
    public void dependency(String id, String versions) {
        getDependencies().put(id, versions);
    }

    /**
     * Declares a dependency on another plugin, which does not need to be installed.
     *
     * @param id       the id of the other plugin.
     * @param versions the range of versions of the other plugin, e.g. [1.0,2).
     */
    public void optionalDependency(String id, String versions) {
        getDependencies().put(id, versions);
        getOptionalDependencies().add(id);
    }

    /**
     * Overrides the range of compatible versions for an included library,
     * e.g. {@code libraryVersions 'com.google.guava:guava', '[32,34)'}.
     * When multiple plugins include the same library, HeadlessMc uses the highest version
     * contained in the ranges of all plugins and refuses to load them if there is none.
     *
     * @param module   group:artifact of the library.
     * @param versions the range of compatible versions.
     */
    public void libraryVersions(String module, String versions) {
        getLibraryVersions().put(module, versions);
    }

}

package io.github.headlesshq.headlessmc.gradle

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

/**
 * Describes the plugin, everything here ends up in META-INF/headlessmc-plugin.json.
 */
abstract class HeadlessMcPluginExtension {
    /**
     * The HeadlessMc version to compile against, e.g. 3.0.0-RC9.
     */
    abstract Property<String> getHeadlessmcVersion()

    /**
     * The unique id of the plugin: lowercase letters, digits, - and _.
     */
    abstract Property<String> getId()

    /**
     * A human-readable name.
     */
    abstract Property<String> getName()

    abstract Property<String> getDescription()

    abstract ListProperty<String> getAuthors()

    /**
     * The license, preferably an SPDX identifier like MIT.
     */
    abstract Property<String> getLicense()

    /**
     * A link to the homepage or the source code.
     */
    abstract Property<String> getHomepage()

    /**
     * The range of HeadlessMc versions the plugin is compatible with,
     * Maven range syntax, e.g. [3.0.0-RC9,4). A bare version means up to the next major version.
     * Defaults to the headlessmcVersion up to the next major version.
     */
    abstract Property<String> getHeadlessmc()

    /**
     * The minimum Java version the plugin needs, at least 25.
     */
    abstract Property<Integer> getJava()

    /**
     * Plugin id to [versions: range, optional: boolean].
     */
    abstract MapProperty<String, Map<String, Object>> getDependencies()

    /**
     * group:artifact to the range of versions of an included library this plugin is compatible with.
     * Libraries not listed here default to [version, nextMajor).
     */
    abstract MapProperty<String, String> getLibraryVersions()

    /**
     * Declares a required dependency on another plugin.
     *
     * @param id the id of the other plugin.
     * @param versions the range of versions of the other plugin, e.g. [1.0,2).
     */
    void dependency(String id, String versions) {
        dependencies.put(id, [versions: versions, optional: false])
    }

    /**
     * Declares a dependency on another plugin, which does not need to be installed.
     */
    void optionalDependency(String id, String versions) {
        dependencies.put(id, [versions: versions, optional: true])
    }

    /**
     * Overrides the range of compatible versions for an included library,
     * e.g. libraryVersions('com.google.guava:guava', '[32,34)').
     * When multiple plugins include the same library, HeadlessMc uses the highest version
     * contained in the ranges of all plugins and refuses to load them if there is none.
     */
    void libraryVersions(String module, String versions) {
        libraryVersions.put(module, versions)
    }

}

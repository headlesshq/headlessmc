package io.github.headlesshq.headlessmc.plugins;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The result of {@link PluginResolver#resolve(List)}.
 *
 * @param plugins   the plugins that can be loaded.
 * @param libraries the nested libraries to load with them, one version per library.
 * @param problems  why plugins cannot be loaded, empty if all can.
 */
public record PluginResolution(List<PluginFile> plugins, List<Library> libraries, List<Problem> problems) {
    /**
     * @param file the plugin file.
     * @return the problems of the given plugin.
     */
    public List<Problem> problemsOf(PluginFile file) {
        return problems.stream().filter(problem -> file.equals(problem.plugin())).toList();
    }

    /**
     * A nested library selected to be loaded.
     *
     * @param library     the library entry of the plugin that ships the selected version.
     * @param source      the plugin jar the library is extracted from.
     * @param requestedBy the ids of all plugins that ship the library.
     */
    public record Library(PluginManifest.Library library, PluginFile source, List<String> requestedBy) {

    }

    /**
     * A reason why a plugin cannot be loaded.
     *
     * @param plugin  the plugin, or {@code null} if the problem concerns no specific plugin.
     * @param message describes the problem.
     */
    public record Problem(@Nullable PluginFile plugin, String message) {
        @Override
        public String toString() {
            return plugin == null ? message : plugin.fileName() + ": " + message;
        }

    }

}

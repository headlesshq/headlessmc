package io.github.headlesshq.headlessmc.mods.modrinth;

import java.util.List;
import java.util.Optional;

/**
 * Represents a Modrinth version.
 *
 * @param game_versions list of versions of Minecraft that this version supports.
 * @param loaders       The mod loaders that this version supports. In case of resource packs, use “minecraft”.
 * @param files         A list of files available for download for this version.
 * @see <a href=https://docs.modrinth.com/api/operations/getversion/>
 * https://docs.modrinth.com/api/operations/getversion/
 * </a>
 */
record ModrinthProjectVersion(
    List<String> game_versions,
    List<String> loaders,
    List<ModrinthFile> files
) {
    public List<String> gameVersions() {
        return game_versions;
    }

    public Optional<ModrinthFile> getPrimaryFile() {
        return files.stream()
            .filter(ModrinthFile::primary)
            .findFirst()
            .or(() -> files.stream().findFirst());
    }

    /*
     TODO: field
      dependencies
        A list of specific versions of projects that this version depends on

     TODO: field
        environment
            required

            The environment a project or version supports. For an explanation of each environment, see the blog post here: https://modrinth.com/news/article/new-environments/#new-system
            string
            Allowed values: client_and_server client_only client_only_server_optional singleplayer_only server_only server_only_client_optional dedicated_server_only client_or_server client_or_server_prefers_both unknown

            client_and_server
     */

}

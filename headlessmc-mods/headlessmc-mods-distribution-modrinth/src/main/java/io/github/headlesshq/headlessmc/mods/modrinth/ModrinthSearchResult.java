package io.github.headlesshq.headlessmc.mods.modrinth;

import java.util.List;

/**
 * Represents the result from a search on modrinth.
 *
 * @param hits the results.
 * @see <a href=https://docs.modrinth.com/api/operations/searchprojects/>
 * https://docs.modrinth.com/api/operations/searchprojects/
 * </a>
 */
record ModrinthSearchResult(List<ModrinthProject> hits) {

}

package io.github.headlesshq.headlessmc.mods.modrinth;

import java.util.Map;

/**
 * Represents the {@link ModrinthProjectVersion#files()} entry of a Modrinth project version.
 *
 * @param hashes   A map of hashes of the file.
 *                 The key is the hashing algorithm and the value is the string version of the hash.
 * @param url      A direct link to the file
 * @param filename The name of the file
 * @param primary  Whether this file is the primary one for its version.
 *                 Only a maximum of one file per version will have this set to true.
 *                 If there are not any primary files,
 *                 it can be inferred that the first file is the primary one.
 * @param size     The size of the file in bytes
 *
 * @see <a href=https://docs.modrinth.com/api/operations/getversion/>
 * https://docs.modrinth.com/api/operations/getversion/
 * </a>
 */
record ModrinthFile(Map<String, String> hashes, String url, String filename, boolean primary, long size) {
    // TODO:  file_type
    //  string
    //  Allowed values: required-resource-pack optional-resource-pack sources-jar dev-jar javadoc-jar unknown signature
    //  nullable

}

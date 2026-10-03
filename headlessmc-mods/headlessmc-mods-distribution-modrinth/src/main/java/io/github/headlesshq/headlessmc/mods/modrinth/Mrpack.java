package io.github.headlesshq.headlessmc.mods.modrinth;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Represents the {@code .mrpack modrinth.index.json} format.
 *
 * @param formatVersion The version of the format, stored as a number.
 *                      The current value at the time of writing is 1.
 * @param versionId     A unique identifier for this specific
 *                      version of the modpack.
 * @param name          Human-readable name of the modpack.
 * @param summary       (optional) A short description of this modpack.
 * @param files         The files array contains a list of files for
 *                      the modpack that needs to be downloaded.
 * @param dependencies  This object contains a list of IDs and version numbers
 *                      that launchers will use in order to know what to install.
 *                      An example dependencies object:
 *                      <pre>{@code
 *                      "dependencies": {
 *                          "minecraft": "1.18.2",
 *                          "forge": "40.1.0"
 *                      }}</pre>
 * @see <a href=https://support.modrinth.com/en/articles/8802351-modrinth-modpack-format-mrpack>
 * https://support.modrinth.com/en/articles/8802351-modrinth-modpack-format-mrpack</a>
 */
@RegisterForReflection(targets = Mrpack.class)
record Mrpack(
    int formatVersion,
    String versionId,
    String name,
    @Nullable String summary,
    List<File> files,
    Map<String, String> dependencies
) implements ReflectionRegistered {
    /**
     * Files for the modpack that need to be downloaded.
     *
     * @param path      The destination path of this file,
     *                  relative to the Minecraft instance directory.
     * @param hashes    The hashes of the file specified. This MUST contain the SHA1 hash and the SHA512 hash.
     *                  Other hashes are optional,
     *                  but will usually be ignored.
     * @param env       For files that only exist on a specific environment,
     *                  this field allows that to be specified.
     *                  It's an object which contains a client and server value.
     *                  This uses the Modrinth client/server type
     *                  specifications.
     *                  Both side types can only be the following values: required, optional, unsupported.
     *                  For example:
     *                  <pre>{@code
     * "env": {
     *     "client": "required",
     *     "server": "unsupported"
     * }
     * }</pre>
     * @param downloads An array containing HTTPS URLs where this file may be downloaded.
     *                  URIs MUST NOT contain unencoded spaces or any other illegal characters according to RFC 3986.
     * @param fileSize  An integer containing the size of the file, in bytes.
     */
    @RegisterForReflection(targets = Mrpack.File.class)
    record File(
        String path,
        Map<String, String> hashes,
        @Nullable Map<String, String> env, // required, optional, unsupported.
        List<URI> downloads,
        long fileSize
    ) implements ReflectionRegistered {}

    // overrides
    // server-overrides
    // client-overrides

}

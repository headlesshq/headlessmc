package io.github.headlesshq.headlessmc.mods.curseforge;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a file of a CurseForge mod.
 *
 * @param id           the id of the file.
 * @param fileName     the name of the file.
 * @param fileDate     when the file was uploaded.
 * @param fileLength   the size of the file in bytes.
 * @param downloadUrl  where to download the file from, {@code null} if the author
 *                     does not allow third party downloads.
 * @param hashes       the hashes of the file.
 * @param gameVersions the game versions, mod loaders and environments this file supports,
 *                     e.g. {@code ["1.21.1", "Fabric", "Client"]}.
 * @see <a href=https://docs.curseforge.com/rest-api/#tocS_File>https://docs.curseforge.com/rest-api/#tocS_File</a>
 */
record CurseForgeFile(
    long id,
    String fileName,
    Instant fileDate,
    long fileLength,
    @Nullable String downloadUrl,
    List<Hash> hashes,
    List<String> gameVersions
) {
    public boolean supports(String gameVersionOrLoader) {
        return gameVersions.stream().anyMatch(gameVersionOrLoader::equalsIgnoreCase);
    }

    /**
     * @return the hashes of this file as algorithm name to digest.
     */
    public Map<String, @Nullable String> hashesByAlgorithm() {
        Map<String, @Nullable String> result = new HashMap<>();
        for (Hash hash : hashes) {
            switch (hash.algo()) {
                case Hash.SHA1 -> result.put("SHA-1", hash.value());
                case Hash.MD5 -> result.put("MD5", hash.value());
                default -> { }
            }
        }

        return result;
    }

    /**
     * A hash of a {@link CurseForgeFile}.
     *
     * @param value the digest.
     * @param algo  the algorithm, {@link #SHA1} or {@link #MD5}.
     */
    record Hash(String value, int algo) {
        public static final int SHA1 = 1;
        public static final int MD5 = 2;
    }

}

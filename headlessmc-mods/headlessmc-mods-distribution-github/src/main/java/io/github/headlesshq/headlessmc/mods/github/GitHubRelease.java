package io.github.headlesshq.headlessmc.mods.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Represents a GitHub release.
 *
 * @param tagName the name of the tag of the release.
 * @param assets  the files attached to the release.
 * @see <a href=https://docs.github.com/en/rest/releases/releases#get-the-latest-release>
 * https://docs.github.com/en/rest/releases/releases#get-the-latest-release
 * </a>
 */
@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRelease(
    @JsonProperty("tag_name") String tagName,
    @Nullable List<Asset> assets
) {
    /**
     * Finds the asset with the given name.
     *
     * @param name the name of the asset.
     * @return the asset with the given name, or empty if the release has no such asset.
     */
    public Optional<Asset> getAsset(String name) {
        return assets == null
            ? Optional.empty()
            : assets.stream().filter(asset -> name.equals(asset.name())).findFirst();
    }

    /**
     * A file attached to a {@link GitHubRelease}.
     *
     * @param name        the name of the file.
     * @param downloadUrl the url to download the file from.
     * @param size        the size of the file in bytes.
     * @param digest      the digest of the file, e.g. {@code sha256:<hex>}.
     */
    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Asset(
        String name,
        @JsonProperty("browser_download_url") String downloadUrl,
        @Nullable Long size,
        @Nullable String digest
    ) {
        private static final String SHA_256_PREFIX = "sha256:";

        /**
         * @return the SHA-256 hex string of this asset, if GitHub provided one.
         */
        public Optional<String> sha256() {
            return digest != null && digest.startsWith(SHA_256_PREFIX)
                ? Optional.of(digest.substring(SHA_256_PREFIX.length()))
                : Optional.empty();
        }
    }

}

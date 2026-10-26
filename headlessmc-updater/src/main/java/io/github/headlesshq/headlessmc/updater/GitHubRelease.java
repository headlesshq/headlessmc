package io.github.headlesshq.headlessmc.updater;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;

/**
 * Represents a GitHub release.
 *
 * @param tagName the name of the tag of the release, which is the version of HeadlessMc.
 * @param htmlUrl the url of the release page.
 * @param assets  the files attached to the release.
 * @see <a href=https://docs.github.com/en/rest/releases/releases#get-the-latest-release>
 * https://docs.github.com/en/rest/releases/releases#get-the-latest-release
 * </a>
 */
public record GitHubRelease(
    @JsonProperty("tag_name") String tagName,
    @JsonProperty("html_url") String htmlUrl,
    List<Asset> assets
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
     */
    public record Asset(String name, @JsonProperty("browser_download_url") String downloadUrl) {

    }

}

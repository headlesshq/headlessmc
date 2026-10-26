package io.github.headlesshq.headlessmc.updater;

import io.github.headlesshq.headlessmc.HeadlessMc;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A newer release of HeadlessMc than the one currently running.
 *
 * @param currentVersion the version of the running HeadlessMc.
 * @param release        the newer release.
 * @param installation   the installation of the running HeadlessMc.
 * @param asset          the asset of the release that replaces the {@link Installation#path()},
 *                       or {@code null} if the release contains no asset for this installation.
 * @param windows        whether HeadlessMc runs on Windows.
 */
public record Update(
    String currentVersion,
    GitHubRelease release,
    Installation installation,
    GitHubRelease.@Nullable Asset asset,
    boolean windows
) {
    /**
     * Creates a message that informs the user about this update,
     * and if possible tells them a command to replace the current installation with it.
     *
     * @return the lines of the message.
     */
    public List<String> getMessage() {
        List<String> lines = new ArrayList<>();
        lines.add("A new version of " + HeadlessMc.NAME + " is available: "
            + currentVersion + " -> " + release.tagName());
        lines.add("Release: " + release.htmlUrl());
        Path path = installation.path();
        if (asset == null) {
            return lines;
        } else if (path == null) {
            lines.add("Download: " + asset.downloadUrl());
            return lines;
        }

        Path download = path.resolveSibling(path.getFileName() + ".new");
        if (windows) {
            lines.add("To update " + path + ", exit " + HeadlessMc.NAME + " and run in PowerShell:");
            lines.add("  Invoke-WebRequest -Uri " + powerShellQuote(asset.downloadUrl())
                + " -OutFile " + powerShellQuote(download.toString())
                + "; Move-Item -Force " + powerShellQuote(download.toString())
                + " " + powerShellQuote(path.toString()));
        } else {
            lines.add("To update " + path + " run:");
            lines.add("  curl -fL -o " + shellQuote(download.toString())
                + " " + shellQuote(asset.downloadUrl())
                + (installation.type() == Installation.Type.NATIVE ? " && chmod +x " + shellQuote(download.toString()) : "")
                + " && mv -f " + shellQuote(download.toString())
                + " " + shellQuote(path.toString()));
        }

        return lines;
    }

    /**
     * @return the lines of {@link #getMessage()}, joined by line separators.
     */
    public String getMessageString() {
        return String.join(System.lineSeparator(), getMessage());
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static String powerShellQuote(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

}

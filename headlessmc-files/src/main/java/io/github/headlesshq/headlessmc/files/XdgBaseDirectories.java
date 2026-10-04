package io.github.headlesshq.headlessmc.files;

import io.github.headlesshq.headlessmc.os.OS;

import java.nio.file.Path;

/**
 * The base directories of the
 * <a href=https://specifications.freedesktop.org/basedir-spec/latest/>
 * XDG Base Directory Specification</a>.
 * <p>
 * The {@code XDG_*_HOME} environment variables are honoured on every
 * {@link OS}, including Windows and macOS, so that users who set them get
 * what they asked for.
 * If a variable is unset, empty, or holds a relative path
 * (which the specification requires us to ignore), the platform default is
 * used instead:
 * <table>
 *     <caption>Defaults per operating system</caption>
 *     <tr><th></th><th>Windows</th><th>macOS</th><th>other</th></tr>
 *     <tr><td>{@link #dataHome()}</td>
 *         <td>{@code %APPDATA%}</td>
 *         <td>{@code ~/Library/Application Support}</td>
 *         <td>{@code ~/.local/share}</td></tr>
 *     <tr><td>{@link #configHome()}</td>
 *         <td>{@code %APPDATA%}</td>
 *         <td>{@code ~/Library/Application Support}</td>
 *         <td>{@code ~/.config}</td></tr>
 *     <tr><td>{@link #stateHome()}</td>
 *         <td>{@code %LOCALAPPDATA%\state}</td>
 *         <td>{@code ~/Library/Application Support}</td>
 *         <td>{@code ~/.local/state}</td></tr>
 *     <tr><td>{@link #cacheHome()}</td>
 *         <td>{@code %LOCALAPPDATA%\cache}</td>
 *         <td>{@code ~/Library/Caches}</td>
 *         <td>{@code ~/.cache}</td></tr>
 * </table>
 * On Windows the roaming {@code %APPDATA%} is used for data and config,
 * because that is where Minecraft itself lives, while the machine local
 * {@code %LOCALAPPDATA%} holds the directories that are not worth roaming.
 *
 * @param dataHome   the base directory for user specific data files.
 * @param configHome the base directory for user specific configuration files.
 * @param stateHome  the base directory for user specific state files,
 *                   e.g. logs or history.
 * @param cacheHome  the base directory for user specific,
 *                   non-essential (cached) data.
 */
public record XdgBaseDirectories(Path dataHome, Path configHome, Path stateHome, Path cacheHome) {
    public static final String XDG_DATA_HOME = "XDG_DATA_HOME";
    public static final String XDG_CONFIG_HOME = "XDG_CONFIG_HOME";
    public static final String XDG_STATE_HOME = "XDG_STATE_HOME";
    public static final String XDG_CACHE_HOME = "XDG_CACHE_HOME";
    public static final String APPDATA = "APPDATA";
    public static final String LOCALAPPDATA = "LOCALAPPDATA";

}

package io.github.headlesshq.headlessmc.version.util;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents a template string that occurs in a version.json.
 * For example {@code "${arch}"} will be replaced by the bitness
 * of your CPU architecture in a version.json file.
 * See {@link TemplateStrings} for more examples.
 *
 * @param name         the name of this template string, e.g. {@code "arch"}
 * @param feature      {@code null} or the feature this template string is connected to,
 *                     see {@link Features}.
 * @param dependencies names of other TemplateStrings this template string requires,
 *                     see {@link #WIDTH}/{@link #HEIGHT}.
 * @see TemplateStrings
 */
public record TemplateString(
    String name,
    @Nullable Feature feature,
    Set<String> dependencies
) {
    /**
     * Regex pattern that captures template strings of the format:
     * {@code "${...}"}.
     */
    @SuppressWarnings("RegExpRedundantEscape") // no, Android might not be able to handle this
    public static final Pattern REGEX = Pattern.compile("(\\$\\{[^\\}]+\\})");

    /** 32/64 bit */
    public static final TemplateString ARCH = new TemplateString("arch");
    /** The asset index name, version.getAssetIndex.getId */
    public static final TemplateString ASSET_INDEX_NAME = new TemplateString("assets_index_name");
    /** The assets folder, e.g. ~/.minecraft/assets */
    public static final TemplateString ASSETS_ROOT = new TemplateString("assets_root");
    /** Account access token */
    public static final TemplateString AUTH_ACCESS_TOKEN = new TemplateString("auth_access_token");
    /** Account name */
    public static final TemplateString AUTH_PLAYER_NAME = new TemplateString("auth_player_name");
    /** Legacy, versions up to 1.6.4, similar to auth_access_token probably, but yggdrasil */
    public static final TemplateString AUTH_SESSION = new TemplateString("auth_session");
    /** Account UUID */
    public static final TemplateString AUTH_UUID = new TemplateString("auth_uuid");
    /** Telemetry, can be left empty, xbox id from entitlements in Microsoft store */
    public static final TemplateString AUTH_XUID = new TemplateString("auth_xuid");
    /** The classpath for java */
    public static final TemplateString CLASSPATH = new TemplateString("classpath");
    /** {@link File#pathSeparator} */
    public static final TemplateString CLASSPATH_SEPARATOR = new TemplateString("classpath_separator");
    /** Telemetry, can be left empty */
    public static final TemplateString CLIENT_ID = new TemplateString("clientid");
    /** Legacy, --assetsDir, for legacy asset index */
    public static final TemplateString GAME_ASSETS = new TemplateString("game_assets");
    /** Wherever we launch the game, e.g. ~/.minecraft */
    public static final TemplateString GAME_DIRECTORY = new TemplateString("game_directory");
    /** Name of the launcher */
    public static final TemplateString LAUNCHER_NAME = new TemplateString("launcher_name");
    /** Version of the launcher */
    public static final TemplateString LAUNCHER_VERSION = new TemplateString("launcher_version");
    /** Directory the library files are in, e.g. .minecraft/libraries */
    public static final TemplateString LIBRARY_DIRECTORY = new TemplateString("library_directory");
    /** Directory the native libraries have been extracted to */
    public static final TemplateString NATIVES_DIRECTORY = new TemplateString("natives_directory");
    /** {}, I think we have this from the wiki, but there are no versions that use this. */
    public static final TemplateString PROFILE_PROPERTIES = new TemplateString("profile_properties");
    /** {}, I think we have this from the wiki, but there are no versions that use this. */
    public static final TemplateString USER_PROPERTIES = new TemplateString("user_properties");
    /** The account type, e.g. msa or legacy. */
    public static final TemplateString USER_TYPE = new TemplateString("user_type");
    /** Version.getId, e.g. 1.12.2 */
    public static final TemplateString VERSION_NAME = new TemplateString("version_name");
    /** Version.getType, release, e.g. 1.12.2 */
    public static final TemplateString VERSION_TYPE = new TemplateString("version_type");

    /** For logging -Dlog4j.configurationFile=${path} */
    public static final TemplateString LOGGING_PATH = new TemplateString("path");

    /** To specify the height of a custom resolution. */
    public static final TemplateString WIDTH = new TemplateString("resolution_width", Feature.CUSTOM_RESOLUTION, "resolution_height");
    /** To specify the width of a custom resolution. */
    public static final TemplateString HEIGHT = new TemplateString("resolution_height", Feature.CUSTOM_RESOLUTION, "resolution_width");

    /** The path to a JSON file containing quick play logs */
    public static final TemplateString QUICK_PLAY_PATH = new TemplateString("quickPlayPath", Feature.QUICK_PLAYS_SUPPORT);
    /** Server to join after launching the game. */
    public static final TemplateString QUICK_PLAY_MULTIPLAYER = new TemplateString("quickPlayMultiplayer", Feature.QUICK_PLAY_MULTIPLAYER);
    /** Realms to join after launching the game */
    public static final TemplateString QUICK_PLAY_REALMS = new TemplateString("quickPlayRealms", Feature.QUICK_PLAY_REALMS);
    /** SinglePlayer world to join after launching the game */
    public static final TemplateString QUICK_PLAY_SINGLEPLAYER = new TemplateString("quickPlaySingleplayer", Feature.QUICK_PLAY_SINGLEPLAYER);

    /**
     * Constructs a new TemplateString for name, no feature
     * and an empty set of dependencies.
     *
     * @param name the name of the TemplateString.
     */
    public TemplateString(String name) {
        this(name, null, Set.of());
    }

    /**
     * Convenience constructor to construct a TemplateString with a set of dependencies.
     *
     * @param name the name of the TemplateString.
     * @param feature the feature enabled by the existence of this template string
     * @param dependencies names of other template strings required by this template string.
     */
    public TemplateString(String name, @Nullable Feature feature, String... dependencies) {
        this(name, feature, Arrays.stream(dependencies).collect(Collectors.toSet()));
    }

    /**
     * Gets {@link #name()} in the format it is used in
     * version.json files for interpolation: {@code "${name}"}
     *
     * @return name as interpolation string.
     */
    public String getTemplateString() {
        return "${" + name + "}";
    }

}

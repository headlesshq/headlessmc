package io.github.headlesshq.headlessmc.launcher.launch;

import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.launcher.auth.LaunchAccount;
import io.github.headlesshq.headlessmc.version.Version;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.io.File;

@UtilityClass
class ArgumentAdapterHelper {
    public static ArgumentAdapter create(Launcher launcher, Version version, String natives, LaunchAccount account) {
        val config = launcher.getConfig();
        val adapter = new ArgumentAdapter(version.getArguments());

        adapter.remove("-cp");
        adapter.remove("${classpath}");
        adapter.remove("-Djava.library.path\\u003d${natives_directory}");

        adapter.add("${natives_directory}", natives);
        adapter.add("${classpath_separator}", File.pathSeparatorChar + "");
        // TODO: check this
        adapter.add("${library_directory}", launcher.getMcFiles().getDir("libraries").getAbsolutePath());

        adapter.add("${version_name}", version.getName());
        adapter.add("${version_type}", "release");
        val dir = new File(config.get(LauncherProperties.GAME_DIR, launcher.getGameDir(version).getPath()));
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();

        adapter.add("${game_directory}", dir.getAbsolutePath());
        adapter.add("${assets_index_name}", version.getAssets());
        adapter.add("${auth_access_token}", account.getToken());
        adapter.add("${user_type}", account.getType());
        adapter.add("${auth_player_name}", account.getName());
        adapter.add("${auth_uuid}", account.getId());
        adapter.add("${assets_root}", launcher.getMcFiles().getDir("assets").getAbsolutePath());

        adapter.add("${launcher_name}", config.get(LauncherProperties.LAUNCHER_NAME, "HeadlessMc"));
        adapter.add("${launcher_version}", config.get(LauncherProperties.LAUNCHER_VERSION, Launcher.VERSION));

        // TODO: these!
        adapter.add("${auth_xuid}", config.get(LauncherProperties.XUID, ""));
        adapter.add("${clientid}", config.get(LauncherProperties.CLIENT_ID, ""));

        // TODO: this is {} for the normal launcher too
        adapter.add("${user_properties}", config.get(LauncherProperties.USER_PROPERTIES, "{}"));
        // TODO: find a version where this actually exists
        adapter.add("${profile_properties}", config.get(LauncherProperties.PROFILE_PROPERTIES, "{}"));

        return adapter;
    }

}

package io.github.headlesshq.headlessmc.launcher;

import io.github.headlesshq.headlessmc.api.config.HmcProperties;
import io.github.headlesshq.headlessmc.api.config.Property;
import io.github.headlesshq.headlessmc.launcher.files.ConfigService;

import static io.github.headlesshq.headlessmc.api.config.PropertyTypes.*;

/**
 * Properties used by the launcher. These can be set as SystemProperties or in
 * the config file managed by the {@link ConfigService}.
 */
public interface LauncherProperties extends HmcProperties {
    Property<String> MC_DIR = string("hmc.mcdir");
    Property<String> GAME_DIR = string("hmc.gamedir");

    Property<String[]> JAVA = array("hmc.java.versions", ";");

    Property<String[]> JVM_ARGS = array("hmc.jvmargs", " ");
    Property<String[]> GAME_ARGS = array("hmc.gameargs", " ");
    Property<String> CLASS_PATH = string("hmc.additional.classpath");
    Property<String> CUSTOM_MAIN_CLASS = string("hmc.main.class");

    Property<String> LAUNCHER_NAME = string("hmc.launchername");
    Property<String> LAUNCHER_VERSION = string("hmc.launcherversion");

    Property<String> EMAIL = string("hmc.email");
    Property<String> PASSWORD = string("hmc.password");

    Property<Boolean> KEEP_FILES = bool("hmc.keepfiles");
    Property<Boolean> STORE_ACCOUNTS = bool("hmc.store.accounts");

    Property<Boolean> INVERT_LWJGL_FLAG = bool("hmc.invert.lwjgl.flag");
    Property<Boolean> INVERT_PAULS_FLAG = bool("hmc.invert.pauls.flag");
    Property<Boolean> INVERT_QUIT_FLAG = bool("hmc.invert.quit.flag");
    Property<Boolean> INVERT_JNDI_FLAG = bool("hmc.invert.jndi.flag");
    Property<Boolean> INVERT_LOOKUP_FLAG = bool("hmc.invert.lookup.flag");

    Property<Boolean> ALWAYS_LWJGL_FLAG = bool("hmc.always.lwjgl.flag");
    Property<Boolean> ALWAYS_PAULS_FLAG = bool("hmc.always.pauls.flag");
    Property<Boolean> ALWAYS_QUIT_FLAG = bool("hmc.always.quit.flag");
    Property<Boolean> ALWAYS_JNDI_FLAG = bool("hmc.always.jndi.flag");
    Property<Boolean> ALWAYS_LOOKUP_FLAG = bool("hmc.always.lookup.flag");

    Property<Boolean> JOML_NO_UNSAFE = bool("hmc.joml.no.unsafe");

    Property<String> XUID = string("hmc.xuid");
    Property<String> CLIENT_ID = string("hmc.clientId");
    Property<String> USER_PROPERTIES = string("hmc.userproperties");
    Property<String> PROFILE_PROPERTIES = string("hmc.profileproperties");

    Property<String> FABRIC_URL = string("hmc.fabric.url");
    Property<Boolean> RE_THROW_LAUNCH_EXCEPTIONS = bool("hmc.rethrow.launch.exceptions");

    Property<Boolean> OFFLINE = bool("hmc.offline");

    Property<String> OFFLINE_TYPE = string("hmc.offline.type");
    Property<String> OFFLINE_USERNAME = string("hmc.offline.username");
    Property<String> OFFLINE_UUID = string("hmc.offline.uuid");
    Property<String> OFFLINE_TOKEN = string("hmc.offline.token");

    Property<Long> ASSETS_DELAY = number("hmc.assets.delay");
    Property<Long> ASSETS_RETRIES = number("hmc.assets.retries");
    Property<Boolean> ASSETS_PARALLEL = bool("hmc.assets.parallel");
    Property<Boolean> DUMMY_ASSETS = bool("hmc.assets.dummy");
    Property<Boolean> ASSETS_CHECK_HASH = bool("hmc.assets.check.hash");
    Property<Boolean> ASSETS_CHECK_SIZE = bool("hmc.assets.check.size"); // < implied by check hash
    Property<Boolean> ASSETS_CHECK_FILE_HASH = bool("hmc.assets.check.file.hash");
    Property<Boolean> ASSETS_BACKOFF = bool("hmc.assets.backoff");
    Property<Boolean> ALWAYS_DOWNLOAD_ASSETS_INDEX = bool("hmc.always.download.assets.index");

    Property<Boolean> LIBRARIES_CHECK_HASH = bool("hmc.libraries.check.hash");
    Property<Boolean> LIBRARIES_CHECK_SIZE = bool("hmc.libraries.check.size"); // < implied by check hash
    Property<Boolean> LIBRARIES_CHECK_FILE_HASH = bool("hmc.libraries.check.file.hash");

    Property<Boolean> SET_LIBRARY_DIR = bool("hmc.set.library.dir");
    Property<Boolean> NO_AUTO_CONFIG = bool("hmc.no.auto.config");

    Property<Boolean> IN_MEMORY = bool("hmc.in.memory");
    Property<Boolean> IN_MEMORY_REQUIRE_CORRECT_JAVA = bool("hmc.in.memory.require.correct.java");
    Property<Long> ASSUMED_JAVA_VERSION = number("hmc.assumed.java.version");
    Property<Boolean> ALWAYS_IN_MEMORY = bool("hmc.always.in.memory");

    Property<Boolean> REFRESH_ON_LAUNCH = bool("hmc.account.refresh.on.launch");
    Property<Boolean> REFRESH_ON_GAME_LAUNCH = bool("hmc.account.refresh.on.game.launch");
    Property<Boolean> REFRESH_FAILURE_DELETE = bool("hmc.account.delete.on.refresh.failure");
    Property<Boolean> FAIL_LAUNCH_ON_REFRESH_FAILURE = bool("hmc.account.fail.launch.on.refresh.failure");

    // TODO: actual cache for each version?
    Property<String> EXTRACTED_FILE_CACHE_UUID = string("hmc.extracted.file.cache.uuid");

    Property<Boolean> HTTP_USER_AGENT_ENABLED = bool("hmc.http.user.agent.enabled");
    Property<String> HTTP_USER_AGENT = string("hmc.http.user.agent");

    Property<Boolean> GAME_DIR_FOR_EACH_VERSION = bool("hmc.game.dir.for.each.version");

    Property<Boolean> INSTALL_LOGGING = bool("hmc.install.mc.logging");

    Property<Boolean> CHECK_XVFB = bool("hmc.check.xvfb");

    Property<Boolean> USE_CURRENT_JAVA = bool("hmc.java.use.current");
    Property<Boolean> AUTO_DOWNLOAD = bool("hmc.auto.download.versions");
    Property<Boolean> AUTO_DOWNLOAD_JAVA = bool("hmc.auto.download.java");
    Property<Boolean> AUTO_DOWNLOAD_JAVA_THROW_EXCEPTION = bool("hmc.auto.download.java.rethrow.exception");
    Property<Boolean> REQUIRE_EXACT_JAVA = bool("hmc.java.require.exact");
    Property<String> JAVA_DISTRIBUTION = string("hmc.auto.java.distribution");
    Property<Boolean> JAVA_ALWAYS_ADD_FILE_PERMISSIONS = bool("hmc.java.always.add.file.permissions");

    Property<Boolean> CRASH_REPORT_WATCHER = bool("hmc.crash.report.watcher");
    Property<Boolean> CRASH_REPORT_WATCHER_EXIT = bool("hmc.crash.report.watcher.exit");

    Property<Boolean> SERVER_LAUNCH_FOR_EULA = bool("hmc.server.launch.for.eula");
    Property<Boolean> SERVER_ACCEPT_EULA = bool("hmc.server.accept.eula");
    Property<String[]> SERVER_ARGS = array("hmc.server.args", " ");

    Property<Boolean> SERVER_TEST = bool("hmc.server.test");
    Property<Boolean> SERVER_TEST_CACHE = bool("hmc.server.test.cache");
    Property<Boolean> SERVER_TEST_CACHE_USE_MC_DIR = bool("hmc.server.test.cache.use.mc.dir");

    Property<String> SERVER_TEST_DIR = string("hmc.server.test.dir");
    Property<String> SERVER_TEST_NAME = string("hmc.server.test.name");
    Property<String> SERVER_TEST_TYPE = string("hmc.server.test.type");
    Property<String> SERVER_TEST_VERSION = string("hmc.server.test.version");
    Property<String> SERVER_TEST_BUILD = string("hmc.server.test.build");

    Property<String> TEST_FILE = string("hmc.test.filename");
    Property<Boolean> LEAVE_AFTER_TEST = bool("hmc.test.leave.after");
    Property<Boolean> NO_TEST_TIMEOUT = bool("hmc.test.no.timeout");

    Property<Boolean> AUTO_DOWNLOAD_SPECIFICS = bool("hmc.auto.download.specifics");

    Property<Boolean> ARM_FIX_LIBRARIES = bool("hmc.arm.fix.libraries");

}

package io.github.headlesshq.headlessmc.launcher.settings;

public class ServerSettings {


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


}

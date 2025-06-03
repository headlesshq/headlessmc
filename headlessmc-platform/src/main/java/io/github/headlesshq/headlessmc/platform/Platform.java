package io.github.headlesshq.headlessmc.platform;

public interface Platform {
    String getName();

    ModFileReader getModFileReader();

    PlatformDownloader getPlatformDownloader();

    // identify platform for a version

    boolean supportsMods();

}

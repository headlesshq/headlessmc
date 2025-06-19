package io.github.headlesshq.headlessmc.platform;

public interface Platform {
    String getName();

    ModFileReader getModFileReader();

    PlatformDownloader getPlatformDownloader();

    // identify platform for a version

    boolean supportsMods();

    default boolean isServerOnly() {
        return false;
    }

    default boolean isClientOnly() {
        return false;
    }

}

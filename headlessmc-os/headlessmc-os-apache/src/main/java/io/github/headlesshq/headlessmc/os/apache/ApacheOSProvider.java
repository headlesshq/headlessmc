package io.github.headlesshq.headlessmc.os.apache;

import io.github.headlesshq.headlessmc.os.OS;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import org.apache.commons.lang3.SystemUtils;

@Dependent
public class ApacheOSProvider {
    @Produces
    @Dependent
    @ApacheCommons
    public OS.Type getType() {
        if (SystemUtils.IS_OS_FREE_BSD) {
            return OS.Type.FREE_BSD;
        } else if (SystemUtils.IS_OS_SOLARIS) {
            return OS.Type.SOLARIS;
        } else if (SystemUtils.IS_OS_AIX) {
            return OS.Type.AIX;
        } else if (SystemUtils.IS_OS_ANDROID) {
            return OS.Type.ANDROID;
        } else if (SystemUtils.IS_OS_LINUX) {
            return OS.Type.LINUX;
        } else if (SystemUtils.IS_OS_MAC) {
            return OS.Type.MACOS;
        } else if (SystemUtils.IS_OS_WINDOWS) {
            return OS.Type.WINDOWS;
        }

        return OS.Type.UNKNOWN;
    }

    @Produces
    @Dependent
    @ApacheCommons
    public OS getOS(@ApacheCommons OS.Type type) {
        return new OS(
            SystemUtils.OS_NAME,
            type,
            SystemUtils.OS_VERSION
        );
    }

}

package io.github.headlesshq.headlessmc.runtime;

import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.config.ConfigImpl;
import io.github.headlesshq.headlessmc.runtime.reflection.RuntimeReflection;

public interface RuntimeTest {
    default RuntimeReflection getRuntime() {
        System.setProperty(RuntimeProperties.ENABLE_REFLECTION.getName(), "true");
        new RuntimeInitializer().init(ConfigImpl.empty());
        return (RuntimeReflection) HeadlessMc.getInstance();
    }

}

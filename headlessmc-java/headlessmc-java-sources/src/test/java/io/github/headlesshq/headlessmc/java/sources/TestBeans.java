package io.github.headlesshq.headlessmc.java.sources;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.java.JavaConfig;
import io.github.headlesshq.headlessmc.os.OS;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * The beans the java sources need but that are contributed by other modules,
 * pinned to values that make scanning tests reproducible.
 * They are {@link DefaultBean}s, so a {@code @QuarkusComponentTest} that only
 * knows about the classes it lists picks them up, while the full container a
 * {@code @QuarkusTest} boots keeps using the real ones.
 */
public class TestBeans {
    static final OS LINUX = new OS("Linux", OS.Type.LINUX, "6.0");

    @Produces
    @Singleton
    @DefaultBean
    OS os() {
        return LINUX;
    }

    @Produces
    @Singleton
    @DefaultBean
    Holder<JavaConfig> javaConfig() {
        return FakeJavaConfig.holder();
    }

}

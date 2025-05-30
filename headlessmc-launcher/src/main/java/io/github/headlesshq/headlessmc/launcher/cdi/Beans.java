package io.github.headlesshq.headlessmc.launcher.cdi;

import io.github.headlesshq.headlessmc.launcher.util.URLs;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;

public class Beans {
    @Produces
    @AssetsUrl
    public URL getAssetsUrl() {
        return URLs.url("https://resources.download.minecraft.net/");
    }

    @Qualifier
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AssetsUrl {

    }

}

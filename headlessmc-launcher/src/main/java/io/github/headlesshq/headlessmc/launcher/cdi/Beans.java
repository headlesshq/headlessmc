package io.github.headlesshq.headlessmc.launcher.cdi;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URI;

public class Beans {
    @Produces
    @AssetsUrl
    public URI getAssetsUrl() {
        return URI.create("https://resources.download.minecraft.net/");
    }

    @Qualifier
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AssetsUrl {

    }

}

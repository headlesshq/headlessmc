package io.github.headlesshq.headlessmc.launcher.assets;

import io.quarkus.runtime.annotations.RegisterResources;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Getter
@ApplicationScoped
@RegisterResources(globs = {DummyAssets.OGG, DummyAssets.PNG})
public class DummyAssets {
    static final String OGG = "assets/dummy.ogg";
    static final String PNG = "assets/dummy.png";
    // static final String JSON = "assets/dummy.json";

    // Should we do JSON files?
    // Minecraft also has assets in: zip, mcmeta, icns
    // but those might be harder to make dummies for.
    // the translations are JSON, but not all.

    private final Map<String, String> resources = new HashMap<>();

    @Inject
    public DummyAssets() {
        resources.put(".ogg", OGG);
        resources.put(".png", PNG);
    }

    public @Nullable InputStream getResource(String fileName) {
        String name = fileName.toLowerCase(Locale.ENGLISH);
        return resources.entrySet()
            .stream()
            .filter(entry -> name.endsWith(entry.getKey()))
            .max(Comparator.comparingInt(entry -> entry.getKey().length()))
            .map(Map.Entry::getValue)
            .map(resource -> getClass().getClassLoader().getResourceAsStream(resource))
            .orElse(null);
    }

}

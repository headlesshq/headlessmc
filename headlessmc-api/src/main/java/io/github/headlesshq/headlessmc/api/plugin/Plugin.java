package io.github.headlesshq.headlessmc.api.plugin;

import io.github.headlesshq.headlessmc.api.traits.HasName;
import io.github.headlesshq.headlessmc.api.traits.HasDescription;
import io.github.headlesshq.headlessmc.api.Application;
import org.jetbrains.annotations.Unmodifiable;

import java.net.URL;
import java.util.List;

public interface Plugin extends HasName, HasDescription {
    void init(Application application);

    @Unmodifiable
    List<String> getAuthors();

    URL getUrl();

}

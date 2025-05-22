package io.github.headlesshq.headlessmc.launcher.mods;

import lombok.Data;
import io.github.headlesshq.headlessmc.api.traits.HasId;
import io.github.headlesshq.headlessmc.api.traits.HasName;
import io.github.headlesshq.headlessmc.api.traits.HasDescription;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Data
public class Mod implements HasId, HasName, HasDescription {
    private final String name;
    private final int id;
    private final @Nullable String description;
    private final List<String> authors;

    public String getDescription() {
        return description == null ? "" : description;
    }

}

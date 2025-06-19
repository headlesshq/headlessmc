package io.github.headlesshq.headlessmc.mods;

import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Data
public class ModDownload {
    private final String id;
    private final @Nullable String description;
    private final List<String> authors;
    private final @Nullable String icon; // TODO: display icons with kitty in the terminal
    // TODO: modrinth also has rbg color

}

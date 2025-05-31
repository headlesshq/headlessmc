package io.github.headlesshq.headlessmc.launcher.mods.files;

import io.github.headlesshq.headlessmc.launcher.mods.Mod;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ModFile extends Mod {
    private final Path path;

    public ModFile(String name, int id, @Nullable String description, List<String> authors, Path path) {
        super(name, id, description, authors);
        this.path = path;
    }

}

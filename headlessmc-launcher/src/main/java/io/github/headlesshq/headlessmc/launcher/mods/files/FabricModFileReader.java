package io.github.headlesshq.headlessmc.launcher.mods.files;

import com.google.gson.annotations.SerializedName;
import io.github.headlesshq.headlessmc.launcher.util.JsonUtil;
import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class FabricModFileReader implements ModFileReader {
    @Override
    public List<ModFile> read(Supplier<Integer> id, Path path) throws IOException {
        try (JarFile jarFile = new JarFile(path.toFile())) {
            JarEntry entry = jarFile.getJarEntry("fabric.mod.json");
            if (entry == null) {
                return Collections.emptyList();
            }

            try (InputStream is = jarFile.getInputStream(entry);
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                FabricModJson fabricMod = JsonUtil.GSON.fromJson(reader, FabricModJson.class);
                return Collections.singletonList(new ModFile(
                        fabricMod.getId(),
                        id.get(),
                        fabricMod.getDescription(),
                        fabricMod.getAuthors() == null ? Collections.emptyList() : fabricMod.getAuthors(),
                        path
                ));
            }
        }
    }

    @Data
    private static final class FabricModJson {
        @SerializedName("id")
        private final String id;
        @SerializedName("description")
        private final String description;
        @SerializedName("authors")
        private final List<String> authors;
        @SerializedName("depends")
        private final Map<String, String> depends;
    }

}

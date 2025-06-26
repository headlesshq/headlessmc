package io.github.headlesshq.headlessmc.platform.fabric;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import io.github.headlesshq.headlessmc.platform.Mod;
import io.github.headlesshq.headlessmc.platform.ModFileReader;
import io.github.headlesshq.headlessmc.platform.ModImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Fabric
@Named("fabric")
@ApplicationScoped
public class FabricModFileReader implements ModFileReader {
    private static final Gson GSON = new Gson();

    @Override
    public List<Mod> read(Path path) throws IOException {
        try (JarFile jarFile = new JarFile(path.toFile())) {
            JarEntry entry = jarFile.getJarEntry("fabric.mod.json");
            if (entry == null) {
                return Collections.emptyList();
            }

            try (InputStream is = jarFile.getInputStream(entry);
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                FabricModJson fabricMod = GSON.fromJson(reader, FabricModJson.class);
                return Collections.singletonList(new ModImpl(
                        fabricMod.getId(),
                        path,
                        fabricMod.getAuthors() == null ? Collections.emptyList() : fabricMod.getAuthors(),
                        fabricMod.getDescription()
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
        private final @Nullable List<String> authors;
        //@SerializedName("depends")
        //private final Map<String, String> depends; // TODO: can be Map<String String[]> ?!
    }

}

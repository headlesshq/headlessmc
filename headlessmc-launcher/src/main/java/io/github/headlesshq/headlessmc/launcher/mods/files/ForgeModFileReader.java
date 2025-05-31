package io.github.headlesshq.headlessmc.launcher.mods.files;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import io.github.headlesshq.headlessmc.launcher.util.JsonUtil;
import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ForgeModFileReader implements ModFileReader {
    private final boolean mcModInfoEnabled;
    private final String[] tomlNames;

    public ForgeModFileReader(boolean mcModInfoEnabled, String... tomlNames) {
        this.mcModInfoEnabled = mcModInfoEnabled;
        this.tomlNames = tomlNames;
    }

    @Override
    public List<ModFile> read(Supplier<Integer> id, Path path) throws IOException {
        try (JarFile jarFile = new JarFile(path.toFile())) {
            for (String tomlName : tomlNames) {
                JarEntry entry = jarFile.getJarEntry(tomlName);
                if (entry != null) {
                    return readModsToml(id, entry, jarFile, path);
                }
            }

            return mcModInfoEnabled
                    ? readMcModInfo(id, jarFile, path)
                    : Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ModFile> readModsToml(Supplier<Integer> id, JarEntry entry, JarFile jarFile, Path path) throws IOException {
        try (InputStream is = jarFile.getInputStream(entry);
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            try {
                TomlParser tomlParser = new TomlParser();
                CommentedConfig config = tomlParser.parse(reader);
                List<CommentedConfig> mods = config.get("mods");
                List<ModFile> result = new ArrayList<>(mods.size());
                for (CommentedConfig mod : mods) {
                    Object authors = mod.get("authors");
                    List<String> authorList = new ArrayList<>();
                    if (authors instanceof String) {
                        authorList.add((String) authors);
                    } else if (authors instanceof List) {
                        for (Object author : (List<Object>) authors) {
                            authorList.add(author.toString());
                        }
                    }

                    result.add(new ModFile(
                            mod.get("modId"),
                            id.get(),
                            mod.get("description").toString().trim(),
                            authorList,
                            path
                    ));
                }

                return result;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

    private List<ModFile> readMcModInfo(Supplier<Integer> id, JarFile jarFile, Path path) throws IOException {
        JarEntry entry = jarFile.getJarEntry("mcmod.info");
        if (entry == null) {
            return Collections.emptyList();
        }

        try (InputStream is = jarFile.getInputStream(entry);
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            TypeToken<List<McModInfo>> type = new TypeToken<List<McModInfo>>() {};
            List<McModInfo> mcModInfos = JsonUtil.GSON.fromJson(reader, type);
            List<ModFile> modFiles = new ArrayList<>(mcModInfos.size());
            for (McModInfo mcModInfo : mcModInfos) {
                modFiles.add(new ModFile(
                    mcModInfo.getModid(),
                        id.get(),
                        mcModInfo.getDescription(),
                        mcModInfo.getAuthorList(),
                        path
                ));
            }

            return modFiles;
        }
    }

    public static ForgeModFileReader neoforge() {
        return new ForgeModFileReader(false, "META-INF/neoforge.mods.toml", "META-INF/forge.mods.toml");
    }

    public static ForgeModFileReader forge() {
        return new ForgeModFileReader(true, "META-INF/forge.mods.toml");
    }

    @Data
    private static final class McModInfo {
        @SerializedName("modid")
        private final String modid;
        @SerializedName("description")
        private final String description;
        @SerializedName("authorList")
        private final List<String> authorList;
        // @SerializedName("dependencies")
        // private final List<String> dependencies;
    }

}

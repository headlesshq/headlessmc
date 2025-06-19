package io.github.headlesshq.headlessmc.platform.forge;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import io.github.headlesshq.headlessmc.platform.Mod;
import io.github.headlesshq.headlessmc.platform.ModFileReader;
import io.github.headlesshq.headlessmc.platform.ModImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Forge
@ApplicationScoped
public class ForgeModFileReader implements ModFileReader {
    private final boolean mcModInfoEnabled;
    private final String[] tomlNames;

    @Inject
    public ForgeModFileReader() {
        this(true, "META-INF/forge.mods.toml");
    }

    public ForgeModFileReader(boolean mcModInfoEnabled, String... tomlNames) {
        this.mcModInfoEnabled = mcModInfoEnabled;
        this.tomlNames = tomlNames;
    }

    @Override
    public List<Mod> read(Path path) throws IOException {
        try (JarFile jarFile = new JarFile(path.toFile())) {
            for (String tomlName : tomlNames) {
                JarEntry entry = jarFile.getJarEntry(tomlName);
                if (entry != null) {
                    return readModsToml(entry, jarFile, path);
                }
            }

            return mcModInfoEnabled
                    ? readMcModInfo(jarFile, path)
                    : Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Mod> readModsToml(JarEntry entry, JarFile jarFile, Path path) throws IOException {
        try (InputStream is = jarFile.getInputStream(entry);
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            try {
                TomlParser tomlParser = new TomlParser();
                CommentedConfig config = tomlParser.parse(reader);
                List<CommentedConfig> mods = config.get("mods");
                List<Mod> result = new ArrayList<>(mods.size());
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

                    result.add(new ModImpl(
                            mod.get("modId"),
                            path,
                            authorList,
                            mod.get("description").toString().trim()
                    ));
                }

                return result;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

    private List<Mod> readMcModInfo(JarFile jarFile, Path path) throws IOException {
        JarEntry entry = jarFile.getJarEntry("mcmod.info");
        if (entry == null) {
            return Collections.emptyList();
        }

        try (InputStream is = jarFile.getInputStream(entry);
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            TypeToken<List<McModInfo>> type = new TypeToken<List<McModInfo>>() {};
            List<McModInfo> mcModInfos = JsonUtil.GSON.fromJson(reader, type);
            List<Mod> modFiles = new ArrayList<>(mcModInfos.size());
            for (McModInfo mcModInfo : mcModInfos) {
                modFiles.add(new ModImpl(
                        mcModInfo.getModid(),
                        path,
                        mcModInfo.getAuthorList(),
                        mcModInfo.getDescription()
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

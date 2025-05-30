package io.github.headlesshq.headlessmc.launcher.command.forge;

import com.google.gson.JsonElement;
import lombok.CustomLog;
import lombok.val;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.download.AbstractDownloadService;
import io.github.headlesshq.headlessmc.launcher.util.JsonUtil;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static io.github.headlesshq.headlessmc.launcher.util.URLs.url;

@CustomLog
public class ForgeIndexCache extends AbstractDownloadService<ForgeVersion> {
    public static final URL LEX_FORGE_INDICES = url("https://meta.prismlauncher.org/v1/net.minecraftforge/index.json");
    public static final URL NEO_FORGE_INDICES = url("https://meta.prismlauncher.org/v1/net.neoforged/index.json");

    public ForgeIndexCache(Launcher launcher, URL url) {
        super(launcher, url);
    }

    @Override
    protected List<ForgeVersion> read(JsonElement element) throws IOException {
        val array = element.getAsJsonObject().get("versions").getAsJsonArray();
        val result = new ArrayList<ForgeVersion>(array.size());
        for (val je : array) {
            val version = JsonUtil.GSON.fromJson(je, ForgeVersion.class);
            result.add(version);
        }

        result.sort(Comparator.reverseOrder());
        return result;
    }

}

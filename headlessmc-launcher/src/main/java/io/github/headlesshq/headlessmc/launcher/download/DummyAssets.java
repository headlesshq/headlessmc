package io.github.headlesshq.headlessmc.launcher.download;

import io.github.headlesshq.headlessmc.launcher.util.IOUtil;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * If we want to run the game in headless mode
 * we do not need some assets like sounds and textures.
 */
@ApplicationScoped
public class DummyAssets {
    private final Map<String, byte[]> bytes = new HashMap<>();

    public DummyAssets() {
        bytes.put("ogg", readResource("assets/dummy.ogg"));
        bytes.put("png", readResource("assets/dummy.png"));
        bytes.put("json", "{}".getBytes(StandardCharsets.UTF_8));
        // Minecraft also has assets in: zip, mcmeta, icns
        // but those might be harder to make dummies for.
        // the translations are jsons, but not all jsons.
    }

    public byte @Nullable [] getResource(String name) {
        return bytes.get(getFileEnding(name).toLowerCase(Locale.ENGLISH));
    }

    public String getFileEnding(String name) {
        int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == name.length() - 1) {
            return "";
        }

        return name.substring(lastDotIndex + 1);
    }

    @SneakyThrows
    private byte[] readResource(String name) {
        try (InputStream is = DummyAssets.class.getClassLoader().getResourceAsStream(name)) {
            if (is == null) {
                throw new IOException("Failed to find resource " + name);
            }

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            IOUtil.copy(is, os);
            return os.toByteArray();
        }
    }

}

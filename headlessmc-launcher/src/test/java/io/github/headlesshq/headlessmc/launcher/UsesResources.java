package io.github.headlesshq.headlessmc.launcher;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.headlesshq.headlessmc.launcher.util.JsonUtil;
import io.github.headlesshq.headlessmc.launcher.version.DefaultVersionFactory;
import io.github.headlesshq.headlessmc.launcher.version.Version;
import lombok.SneakyThrows;
import lombok.val;

import java.io.File;

public interface UsesResources {
    @SneakyThrows
    default JsonElement getJsonElement(String name) {
        val is = getClass().getClassLoader().getResourceAsStream(name);
        return JsonUtil.fromInput(is);
    }

    @SneakyThrows
    default JsonObject getJsonObject(String name) {
        return getJsonElement(name).getAsJsonObject();
    }

    default Version getVersion(String name, int id) {
        return getVersion(name, "dummy", id);
    }

    @SneakyThrows
    default Version getVersion(String name, String file, int id) {
        val factory = new DefaultVersionFactory();
        return factory.parse(getJsonObject(name), new File(file), () -> id);
    }

}

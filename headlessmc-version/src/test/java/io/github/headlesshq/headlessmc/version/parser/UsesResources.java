package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Cleanup;
import lombok.SneakyThrows;

import java.io.InputStream;

public interface UsesResources {
    @SneakyThrows
    default JsonElement getJsonElement(String name) {
        @Cleanup
        InputStream is = getClass().getClassLoader().getResourceAsStream(name);
        assert is != null;
        return JsonUtil.fromInput(is);
    }

    @SneakyThrows
    default JsonObject getJsonObject(String name) {
        return getJsonElement(name).getAsJsonObject();
    }

}

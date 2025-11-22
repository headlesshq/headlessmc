package io.github.headlesshq.headlessmc.auth;

import com.google.gson.JsonObject;
import lombok.Data;
import io.github.headlesshq.headlessmc.api.HasName;

@Data
public class YggdrasilAccount implements HasName {
    private final String serverUrl;
    private final String accessToken;
    private final String clientToken;
    private final String uuid;
    private final String name;

    @Override
    public String getName() {
        return name;
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "yggdrasil");
        jsonObject.addProperty("serverUrl", serverUrl);
        jsonObject.addProperty("accessToken", accessToken);
        if (clientToken != null) {
            jsonObject.addProperty("clientToken", clientToken);
        }
        jsonObject.addProperty("uuid", uuid);
        jsonObject.addProperty("name", name);
        return jsonObject;
    }

    public static YggdrasilAccount fromJson(JsonObject jsonObject) {
        return new YggdrasilAccount(
            jsonObject.get("serverUrl").getAsString(),
            jsonObject.get("accessToken").getAsString(),
            jsonObject.has("clientToken") ? jsonObject.get("clientToken").getAsString() : null,
            jsonObject.get("uuid").getAsString(),
            jsonObject.get("name").getAsString()
        );
    }

}


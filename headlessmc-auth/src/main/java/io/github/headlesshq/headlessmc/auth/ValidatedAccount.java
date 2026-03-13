package io.github.headlesshq.headlessmc.auth;

import com.google.gson.JsonObject;
import lombok.Data;
import io.github.headlesshq.headlessmc.api.HasName;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;

@Data
public class ValidatedAccount implements HasName {
    private final StepFullJavaSession.FullJavaSession session;
    private final String xuid;

    @Override
    public String getName() {
        return session.getMcProfile().getName();
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("session", MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.toJson(session));
        jsonObject.addProperty("xuid", xuid);
        return jsonObject;
    }

    public static ValidatedAccount fromJson(JsonObject jsonObject) {
        JsonObject sessionJson = jsonObject.get("session").getAsJsonObject();
        StepFullJavaSession.FullJavaSession session = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.fromJson(sessionJson);
        return new ValidatedAccount(session, jsonObject.get("xuid").getAsString());
    }

}

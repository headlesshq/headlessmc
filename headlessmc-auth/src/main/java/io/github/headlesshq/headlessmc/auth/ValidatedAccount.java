package io.github.headlesshq.headlessmc.auth;

import com.google.gson.JsonObject;
import io.github.headlesshq.headlessmc.api.traits.HasName;
import lombok.Data;
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
        StepFullJavaSession.FullJavaSession session = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.fromJson(jsonObject.get("session").getAsJsonObject());
        return new ValidatedAccount(session, jsonObject.get("xuid").getAsString());
    }

}

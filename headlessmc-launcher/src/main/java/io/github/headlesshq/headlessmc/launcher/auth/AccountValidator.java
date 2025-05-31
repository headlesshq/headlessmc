package io.github.headlesshq.headlessmc.launcher.auth;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import io.github.headlesshq.headlessmc.auth.ValidatedAccount;
import io.github.headlesshq.headlessmc.launcher.util.JsonUtil;
import lombok.CustomLog;
import lombok.Data;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.requests.impl.GetRequest;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.responsehandler.MinecraftResponseHandler;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.List;

/**
 * Validates that an account actually owns the game.
 */
@CustomLog
public class AccountValidator {
    private static final URI URL = URI.create("https://api.minecraftservices.com/entitlements/mcstore");

    public ValidatedAccount validate(StepFullJavaSession.FullJavaSession session) throws AuthException {
        log.debug("Validating session " + session.getMcProfile().getName() + " : " + session.getMcProfile().getId());
        try {
            HttpClient httpClient = MinecraftAuth.createHttpClient();
            GetRequest getRequest = new GetRequest(URL);
            getRequest.appendHeader("Authorization", "Bearer " + session.getMcProfile().getMcToken().getAccessToken());
            JsonObject je = httpClient.execute(getRequest, new MinecraftResponseHandler());
            log.debug(je.toString());

            Entitlements entitlements = JsonUtil.GSON.fromJson(je, Entitlements.class);
            String xuid = null;
            for (Entitlements.Item item : entitlements.getItems()) {
                // TODO: is "product_minecraft" really also fine? it also contains the same xuid so it should be?
                if ("game_minecraft".equals(item.getName()) || "product_minecraft".equals(item.getName())) {
                    xuid = item.parseXuid();
                    break;
                }
            }

            if (xuid == null) {
                throw new AuthException("This account does not own Minecraft!");
            }

            return new ValidatedAccount(session, xuid);
        } catch (IOException | JsonParseException e) {
            log.error("Failed to validate " + session.getMcProfile().getName(), e);
            throw new AuthException("Failed to validate " + session.getMcProfile().getName() + ": " + e.getMessage());
        }
    }

    @Data
    @VisibleForTesting
    static class Entitlements {
        @SerializedName("items")
        private List<Item> items;

        @SerializedName("signature")
        private String signature;

        @SerializedName("keyId")
        private String keyId;

        @Data
        static class Item {
            @SerializedName("name")
            private String name;

            @SerializedName("signature")
            private String signature;

            public String parseXuid() throws AuthException, JsonSyntaxException {
                // TODO: also verify signature?
                String[] split = signature.split("\\.");
                if (split.length != 3) {
                    throw new AuthException("Invalid JWT " + signature);
                }

                String payload = new String(Base64.getDecoder().decode(split[1]));
                JsonElement jsonElement = JsonParser.parseString(payload);
                String xuid = JsonUtil.getString(jsonElement, "signerId");
                if (xuid == null) {
                    throw new AuthException("Failed to find xuid for " + signature);
                }

                return xuid;
            }
        }
    }

}

package io.github.headlesshq.headlessmc.auth.impl;

import com.google.gson.JsonElement;
import io.github.headlesshq.headlessmc.auth.store.gson.GsonCodec;
import lombok.RequiredArgsConstructor;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.java.JavaAuthManager;

import java.io.IOException;

@RequiredArgsConstructor
public class AuthenticatorCodec implements GsonCodec<JavaAuthManager> {
    private final HttpClient httpClient;

    @Override
    public JavaAuthManager fromJson(String id, JsonElement element) throws IOException {
        if (!element.isJsonObject()) {
            throw new IOException("Element " + id + " was not a json object");
        }

        return JavaAuthManager.fromJson(httpClient, element.getAsJsonObject());
    }

    @Override
    public JsonElement toJson(String id, JavaAuthManager authManager) {
        return JavaAuthManager.toJson(authManager);
    }

}

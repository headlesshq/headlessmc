package io.github.headlesshq.headlessmc.auth.store.gson;

import com.google.gson.JsonElement;

import java.io.IOException;

public interface GsonCodec<T> {
    T fromJson(String id, JsonElement element) throws IOException;

    JsonElement toJson(String id, T element) throws IOException;

}

package io.github.headlesshq.headlessmc.auth.store.gson;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.lang.reflect.Type;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class GsonCodecImpl<T> implements GsonCodec<T> {
    private final Gson gson = new Gson();
    private final Type type;

    public GsonCodecImpl(Class<T> type) {
        this((Type) type);
    }

    public GsonCodecImpl(TypeToken<T> type) {
        this(type.getType());
    }

    @Override
    public T fromJson(String id, JsonElement element) throws IOException {
        try {
            return gson.fromJson(element, type);
        } catch (JsonParseException e) { // TODO: this could leak secrets?
            throw new IOException("Failed to parse element " + id, e);
        }
    }

    @Override
    public JsonElement toJson(String id, T element) throws IOException {
        try {
            return gson.toJsonTree(element, type);
        } catch (JsonParseException e) { // TODO: this could leak secrets?
            throw new IOException("Failed to parse element " + id, e);
        }
    }

}

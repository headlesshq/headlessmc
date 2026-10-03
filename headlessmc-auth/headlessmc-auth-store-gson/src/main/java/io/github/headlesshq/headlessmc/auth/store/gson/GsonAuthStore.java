package io.github.headlesshq.headlessmc.auth.store.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.headlesshq.headlessmc.auth.AuthStore;
import io.github.headlesshq.headlessmc.auth.AuthStoreException;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;

@RequiredArgsConstructor
public abstract class GsonAuthStore<T> implements AuthStore<T> {
    protected final GsonCodec<T> codec;
    protected final Lock lock;
    protected final long version;

    protected abstract JsonObject readJson() throws IOException;

    protected abstract void writeJson(JsonElement object) throws IOException;

    @Override
    public SortedMap<String, T> read() throws AuthStoreException {
        try {
            lock.lock();
            JsonObject object = readJson();
            JsonElement versionElement = object.get("version");
            if (versionElement != null && (!versionElement.isJsonPrimitive() || versionElement.getAsLong() > version)) {
                throw new AuthStoreException("Failed to read accounts, unsupported version "
                    + versionElement + " > " + version
                    + " either clear accounts, or update to a newer version of HeadlessMc.");
            }

            JsonElement accounts = object.get("accounts");
            if (accounts != null && accounts.isJsonObject()) {
                SortedMap<String, T> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                for (Map.Entry<String, JsonElement> entry : accounts.getAsJsonObject().entrySet()) {
                    result.put(entry.getKey(), codec.fromJson(entry.getKey(), entry.getValue()));
                }

                return result;
            }

            return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        } catch (IOException e) {
            throw new AuthStoreException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void save(Map<String, T> values) throws AuthStoreException {
        try {
            lock.lock();
            JsonObject accounts = new JsonObject();
            for (Map.Entry<String, T> entry : values.entrySet()) {
                accounts.add(entry.getKey(), codec.toJson(entry.getKey(), entry.getValue()));
            }

            JsonObject result = new JsonObject();
            result.add("accounts", accounts);
            result.add("version", new JsonPrimitive(version));
            writeJson(result);
        } catch (IOException e) {
            throw new AuthStoreException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void add(String id, T value) {
        try {
            lock.lock();
            Map<String, T> map = new LinkedHashMap<>(read());
            map.put(id, value);
            save(map);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void remove(String id) throws AuthStoreException {
        try {
            lock.lock();
            Map<String, T> map = new LinkedHashMap<>(read());
            if (map.remove(id) != null) {
                save(map);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<T> getById(String id) throws AuthStoreException {
        try {
            lock.lock();
            Map<String, T> map = read();
            return Optional.ofNullable(map.get(id));
        } finally {
            lock.unlock();
        }
    }

}

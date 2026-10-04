package io.github.headlesshq.headlessmc.auth.store.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.headlesshq.headlessmc.auth.AuthStoreException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class GsonAuthStoreTest {
    @TempDir
    Path tempDir;

    private ReentrantLock lock;
    private GsonCodecImpl<TestValue> codec;

    @BeforeEach
    void setUp() {
        lock = new ReentrantLock();
        codec = new GsonCodecImpl<>(TestValue.class);
    }

    @Test
    void readReturnsEmptyMapWhenFileMissing() {
        TestStore store = new TestStore(codec, lock, 1L, tempDir.resolve("missing.json"));
        assertTrue(store.read().isEmpty());
    }

    @Test
    void saveAndReadRoundTrip() {
        Path file = tempDir.resolve("accounts.json");
        TestStore store = new TestStore(codec, lock, 1L, file);

        Map<String, TestValue> values = new LinkedHashMap<>();
        values.put("a", new TestValue("one", 1));
        values.put("b", new TestValue("two", 2));

        store.save(values);
        Map<String, TestValue> read = store.read();

        assertEquals(2, read.size());
        assertEquals(new TestValue("one", 1), read.get("a"));
        assertEquals(new TestValue("two", 2), read.get("b"));
    }

    @Test
    void getByIdReturnsValue() {
        Path file = tempDir.resolve("accounts.json");
        TestStore store = new TestStore(codec, lock, 1L, file);
        store.add("id", new TestValue("value", 7));

        Optional<TestValue> result = store.getById("id");

        assertTrue(result.isPresent());
        assertEquals(new TestValue("value", 7), result.get());
    }

    @Test
    void removeDeletesEntry() {
        Path file = tempDir.resolve("accounts.json");
        TestStore store = new TestStore(codec, lock, 1L, file);
        store.add("id", new TestValue("value", 7));

        store.remove("id");

        assertTrue(store.getById("id").isEmpty());
    }

    @Test
    void readRejectsUnsupportedVersion() throws IOException {
        Path file = tempDir.resolve("accounts.json");
        JsonObject root = new JsonObject();
        root.add("version", new JsonPrimitive(2L));
        root.add("accounts", new JsonObject());
        Files.createDirectories(file.getParent());
        Files.writeString(file, root.toString());

        TestStore store = new TestStore(codec, lock, 1L, file);

        assertThrows(AuthStoreException.class, store::read);
    }

    @Test
    void readReturnsEmptyMapForInvalidAccountsType() throws IOException {
        Path file = tempDir.resolve("accounts.json");
        JsonObject root = new JsonObject();
        root.add("version", new JsonPrimitive(1L));
        root.add("accounts", new JsonPrimitive("wrong"));
        Files.createDirectories(file.getParent());
        Files.writeString(file, root.toString());

        TestStore store = new TestStore(codec, lock, 1L, file);

        assertTrue(store.read().isEmpty());
    }

    @Test
    void saveWritesVersionAndAccounts() {
        Path file = tempDir.resolve("accounts.json");
        TestStore store = new TestStore(codec, lock, 5L, file);

        Map<String, TestValue> values = new LinkedHashMap<>();
        values.put("id", new TestValue("value", 3));

        store.save(values);

        JsonObject root = store.readRaw();
        assertEquals(5L, root.get("version").getAsLong());
        assertTrue(root.get("accounts").isJsonObject());
        assertEquals("value", root.getAsJsonObject("accounts").getAsJsonObject("id").get("name").getAsString());
    }

    @Test
    void codecThrowsOnInvalidJson() {
        assertThrows(IOException.class, () -> codec.fromJson("id", new JsonPrimitive("not-an-object")));
    }

    @Test
    void codecConvertsToJson() throws IOException {
        JsonElement element = codec.toJson("id", new TestValue("x", 9));
        assertTrue(element.isJsonObject());
        assertEquals("x", element.getAsJsonObject().get("name").getAsString());
    }

    static final class TestStore extends GsonFileAuthStore<TestValue> {
        TestStore(GsonCodec<TestValue> codec, Lock lock, long version, Path file) {
            super(codec, lock, version, file);
        }

        JsonObject readRaw() {
            try {
                return readJson();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @NoArgsConstructor
    static final class TestValue {
        String name;
        int number;

        TestValue(String name, int number) {
            this.name = name;
            this.number = number;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TestValue that)) return false;
            return number == that.number && java.util.Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(name, number);
        }
    }

}

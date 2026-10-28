package io.github.headlesshq.headlessmc.auth.store.gson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;

class GsonFileAuthStoreTest {
    private static GsonFileAuthStore<String> store(Path file) {
        return new GsonFileAuthStore<>(new GsonCodecImpl<>(String.class), new ReentrantLock(), 0L, file);
    }

    @Test
    void holdsNothingInMemoryBeforeReading(@TempDir Path dir) {
        Path file = dir.resolve("accounts.json");
        store(file).save(Map.of("a", "token-a"));

        assertTrue(store(file).loaded().isEmpty());
    }

    @Test
    void holdsWhatWasReadOrWrittenLast(@TempDir Path dir) {
        Path file = dir.resolve("accounts.json");
        GsonFileAuthStore<String> store = store(file);

        store.add("a", "token-a");
        assertEquals(List.of("token-a"), List.copyOf(store.loaded()));

        store(file).add("b", "token-b");
        assertEquals(List.of("token-a"), List.copyOf(store.loaded()));

        store.read();
        assertEquals(List.of("token-a", "token-b"), List.copyOf(store.loaded()));

        store.remove("a");
        assertEquals(List.of("token-b"), List.copyOf(store.loaded()));
    }

}

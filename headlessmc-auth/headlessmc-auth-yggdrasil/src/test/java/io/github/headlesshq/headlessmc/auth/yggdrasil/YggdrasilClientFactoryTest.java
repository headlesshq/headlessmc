package io.github.headlesshq.headlessmc.auth.yggdrasil;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class YggdrasilClientFactoryTest {
    @Test
    void appendsTheApiPathToAServerRoot() {
        assertEquals(URI.create("https://littleskin.cn/api/yggdrasil"),
            YggdrasilClientFactory.toApiUri("https://littleskin.cn"));
    }

    @Test
    void keepsAUrlThatAlreadyPointsAtTheApi() {
        assertEquals(URI.create("https://littleskin.cn/api/yggdrasil"),
            YggdrasilClientFactory.toApiUri("https://littleskin.cn/api/yggdrasil"));
    }

    @Test
    void stripsTrailingSlashes() {
        assertEquals(URI.create("https://littleskin.cn/api/yggdrasil"),
            YggdrasilClientFactory.toApiUri("https://littleskin.cn///"));
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertEquals(URI.create("https://littleskin.cn/api/yggdrasil"),
            YggdrasilClientFactory.toApiUri("  https://littleskin.cn  "));
    }

    @Test
    void keepsAPort() {
        assertEquals(URI.create("http://localhost:8080/api/yggdrasil"),
            YggdrasilClientFactory.toApiUri("http://localhost:8080"));
    }

    @Test
    void rejectsAnEmptyUrl() {
        assertThrows(IllegalArgumentException.class, () -> YggdrasilClientFactory.toApiUri(""));
        assertThrows(IllegalArgumentException.class, () -> YggdrasilClientFactory.toApiUri("   "));
        assertThrows(IllegalArgumentException.class, () -> YggdrasilClientFactory.toApiUri(null));
    }

    @Test
    void metadataIsRequestedFromTheApiRoot() {
        // the metadata lives at the API root, without the /authserver suffix
        assertEquals(URI.create("https://littleskin.cn/api/yggdrasil"),
            YggdrasilClientFactory.toApiUri("https://littleskin.cn"));
    }

}

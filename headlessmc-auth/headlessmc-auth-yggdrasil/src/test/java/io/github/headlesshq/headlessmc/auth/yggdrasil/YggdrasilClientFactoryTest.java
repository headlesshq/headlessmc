package io.github.headlesshq.headlessmc.auth.yggdrasil;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class YggdrasilClientFactoryTest {
    private static final String API_PATH = YggdrasilClientFactory.DEFAULT_API_PATH;

    private static URI toApiUri(String url) {
        return YggdrasilClientFactory.toApiUri(url, API_PATH);
    }

    @Test
    void appendsTheApiPathToAServerRoot() {
        assertEquals(URI.create("https://littleskin.cn/api/yggdrasil"),
            toApiUri("https://littleskin.cn"));
    }

    @Test
    void keepsAUrlThatAlreadyPointsAtTheApi() {
        assertEquals(URI.create("https://littleskin.cn/api/yggdrasil"),
            toApiUri("https://littleskin.cn/api/yggdrasil"));
    }

    @Test
    void stripsTrailingSlashes() {
        assertEquals(URI.create("https://littleskin.cn/api/yggdrasil"),
            toApiUri("https://littleskin.cn///"));
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertEquals(URI.create("https://littleskin.cn/api/yggdrasil"),
            toApiUri("  https://littleskin.cn  "));
    }

    @Test
    void keepsAPort() {
        assertEquals(URI.create("http://localhost:8080/api/yggdrasil"),
            toApiUri("http://localhost:8080"));
    }

    @Test
    void rejectsAnEmptyUrl() {
        assertThrows(IllegalArgumentException.class, () -> toApiUri(""));
        assertThrows(IllegalArgumentException.class, () -> toApiUri("   "));
        assertThrows(IllegalArgumentException.class, () -> toApiUri(null));
    }

    @Test
    void usesAConfiguredApiPath() {
        // servers that do not deploy the API under /api/yggdrasil
        assertEquals(URI.create("https://example.com/custom/yggdrasil"),
            YggdrasilClientFactory.toApiUri("https://example.com", "/custom/yggdrasil"));
    }

    @Test
    void acceptsAnApiPathWithoutLeadingSlash() {
        assertEquals(URI.create("https://example.com/custom/yggdrasil"),
            YggdrasilClientFactory.toApiUri("https://example.com", "custom/yggdrasil"));
    }

    @Test
    void stripsTrailingSlashesFromTheApiPath() {
        assertEquals(URI.create("https://example.com/api/yggdrasil"),
            YggdrasilClientFactory.toApiUri("https://example.com", "/api/yggdrasil/"));
    }

    @Test
    void doesNotAppendThePathTwice() {
        assertEquals(URI.create("https://example.com/custom/yggdrasil"),
            YggdrasilClientFactory.toApiUri(
                "https://example.com/custom/yggdrasil", "/custom/yggdrasil"));
    }

    @Test
    void anEmptyApiPathKeepsTheUrlAsItIs() {
        // servers that serve the API at their root
        assertEquals(URI.create("https://example.com"),
            YggdrasilClientFactory.toApiUri("https://example.com", ""));
        assertEquals(URI.create("https://example.com"),
            YggdrasilClientFactory.toApiUri("https://example.com", "/"));
    }

}

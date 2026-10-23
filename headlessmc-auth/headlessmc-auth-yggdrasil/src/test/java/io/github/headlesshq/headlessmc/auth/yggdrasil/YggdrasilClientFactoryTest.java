package io.github.headlesshq.headlessmc.auth.yggdrasil;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class YggdrasilClientFactoryTest {
    private static final String API_PATH = YggdrasilClientFactory.DEFAULT_API_PATH;
    private static final String LOGIN_PATH = YggdrasilClientFactory.DEFAULT_LOGIN_PATH;

    private static URI toApiUri(String url) {
        return YggdrasilClientFactory.toApiUri(url, API_PATH, LOGIN_PATH);
    }

    @Test
    void appendsThePathsToAServerRoot() {
        assertEquals(URI.create("https://littleskin.cn/api/yggdrasil/authserver"),
            toApiUri("https://littleskin.cn"));
    }

    @Test
    void keepsAUrlThatAlreadyPointsAtTheApi() {
        assertEquals(URI.create("https://littleskin.cn/api/yggdrasil/authserver"),
            toApiUri("https://littleskin.cn/api/yggdrasil"));
    }

    @Test
    void stripsTrailingSlashes() {
        assertEquals(URI.create("https://littleskin.cn/api/yggdrasil/authserver"),
            toApiUri("https://littleskin.cn///"));
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertEquals(URI.create("https://littleskin.cn/api/yggdrasil/authserver"),
            toApiUri("  https://littleskin.cn  "));
    }

    @Test
    void keepsAPort() {
        assertEquals(URI.create("http://localhost:8080/api/yggdrasil/authserver"),
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
        assertEquals(URI.create("https://example.com/custom/yggdrasil/authserver"),
            YggdrasilClientFactory.toApiUri("https://example.com", "/custom/yggdrasil", LOGIN_PATH));
    }

    @Test
    void acceptsAnApiPathWithoutLeadingSlash() {
        assertEquals(URI.create("https://example.com/custom/yggdrasil/authserver"),
            YggdrasilClientFactory.toApiUri("https://example.com", "custom/yggdrasil", LOGIN_PATH));
    }

    @Test
    void stripsTrailingSlashesFromTheApiPath() {
        assertEquals(URI.create("https://example.com/api/yggdrasil/authserver"),
            YggdrasilClientFactory.toApiUri("https://example.com", "/api/yggdrasil/", LOGIN_PATH));
    }

    @Test
    void doesNotAppendTheApiPathTwice() {
        assertEquals(URI.create("https://example.com/custom/yggdrasil/authserver"),
            YggdrasilClientFactory.toApiUri(
                "https://example.com/custom/yggdrasil", "/custom/yggdrasil", LOGIN_PATH));
    }

    @Test
    void usesAConfiguredLoginPath() {
        // servers that do not serve the login endpoints under /authserver
        assertEquals(URI.create("https://example.com/api/yggdrasil/login"),
            YggdrasilClientFactory.toApiUri("https://example.com", API_PATH, "/login"));
    }

    @Test
    void anEmptyApiPathKeepsTheUrlAsItIs() {
        // servers that serve the API at their root
        assertEquals(URI.create("https://example.com/authserver"),
            YggdrasilClientFactory.toApiUri("https://example.com", "", LOGIN_PATH));
        assertEquals(URI.create("https://example.com/authserver"),
            YggdrasilClientFactory.toApiUri("https://example.com", "/", LOGIN_PATH));
    }

    @Test
    void anEmptyLoginPathKeepsOnlyTheApiRoot() {
        // servers that serve the login endpoints at the API root
        assertEquals(URI.create("https://example.com/api/yggdrasil"),
            YggdrasilClientFactory.toApiUri("https://example.com", API_PATH, ""));
        assertEquals(URI.create("https://example.com/api/yggdrasil"),
            YggdrasilClientFactory.toApiUri("https://example.com", API_PATH, "/"));
    }

}

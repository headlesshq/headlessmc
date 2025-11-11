package io.github.headlesshq.headlessmc.launcher.util;

import lombok.SneakyThrows;
import lombok.val;
import io.github.headlesshq.headlessmc.api.util.AbstractUtilityTest;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class URLsTest extends AbstractUtilityTest<URLs> {
    @Test
    @SneakyThrows
    public void testUrls() {
        val url = new URL("https://github.com/headlesshq/HeadlessMc");
        assertEquals(url, URLs.url("https://github.com/headlesshq/HeadlessMc"));
        assertThrows(MalformedURLException.class, () -> URLs.url("test"));
    }

}

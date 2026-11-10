package io.github.headlesshq.headlessmc.plugins;

import io.github.headlesshq.headlessmc.exceptions.RequestException;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluginBootstrapTest {
    @Test
    void testDescribeUsesMessage() {
        assertEquals("boom", PluginBootstrap.describe(new IllegalStateException("boom")));
    }

    @Test
    void testDescribeFallsBackToCause() {
        RequestException e = new RequestException(new ConnectException());
        assertEquals("RequestException: ConnectException", PluginBootstrap.describe(e));
    }

    @Test
    void testDescribeWithoutMessageAndCause() {
        assertEquals("RequestException", PluginBootstrap.describe(new RequestException()));
    }
}

package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.JsonObject;
import io.github.headlesshq.headlessmc.version.Download;
import io.github.headlesshq.headlessmc.version.Logging;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LoggingTest implements UsesResources {
    @Test
    public void testParse() {
        JsonObject element = getJsonObject("logging.json");
        Logging logging = LoggingImpl.getFromVersion(element);
        assertNotNull(logging);
        Assertions.assertEquals("-Dlog4j.configurationFile=${path}", logging.getArgument());
        Assertions.assertEquals("log4j2-xml", logging.getType());
        Download file = logging.getFile();
        Assertions.assertEquals("client-1.12.xml", file.getName());
        Assertions.assertEquals("bd65e7d2e3c237be76cfbef4c2405033d7f91521", file.getSha1());
        Assertions.assertEquals(888, file.getSize());
        Assertions.assertEquals(URI.create("https://piston-data.mojang.com/v1/objects/bd65e7d2e3c237be76cfbef4c2405033d7f91521/client-1.12.xml"), file.getUrl());
    }

}

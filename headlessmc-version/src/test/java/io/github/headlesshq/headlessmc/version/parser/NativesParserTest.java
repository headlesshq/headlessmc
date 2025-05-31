package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class NativesParserTest implements UsesResources {
    @Test
    public void testNativesFactory() {
        JsonElement element = getJsonObject("lib_natives.json").get("natives");
        NativesParser nativesFactory = new NativesParser();
        Assertions.assertTrue(nativesFactory.parse(null).isEmpty());
        Assertions.assertTrue(nativesFactory.parse(new JsonArray()).isEmpty());
        Map<String, String> natives = nativesFactory.parse(element);
        Assertions.assertEquals(1, natives.size());
        Assertions.assertTrue(natives.containsKey("osx"));
        Assertions.assertEquals("natives-osx", natives.get("osx"));
    }

}

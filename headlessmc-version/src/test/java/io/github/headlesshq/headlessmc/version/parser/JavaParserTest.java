package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

public class JavaParserTest {
    private final JavaParser parser = new JavaParser();

    @Test
    public void testParse() {
        assertNull(parser.parse(null));
        assertNull(parser.parse(new JsonArray()));
        JsonObject object = new JsonObject();
        assertNull(parser.parse(object));
        object.add("test", new JsonArray());
        assertNull(parser.parse(object));
        object.add("majorVersion", new JsonPrimitive(8));
        Assertions.assertEquals(8, parser.parse(object));
        object.add("majorVersion", new JsonPrimitive(17));
        Assertions.assertEquals(17, parser.parse(object));
        object.add("majorVersion", new JsonPrimitive(17.0));
        Assertions.assertEquals(17, parser.parse(object));
        object.add("majorVersion", new JsonPrimitive(8));
        Assertions.assertEquals(8, parser.parse(object));
        object.add("majorVersion", new JsonPrimitive("8"));
        Assertions.assertEquals(8, parser.parse(object));
        object.add("majorVersion", new JsonPrimitive("9.0"));
        Assertions.assertEquals(9, parser.parse(object));
        object.add("majorVersion", new JsonPrimitive("17.0"));
        Assertions.assertEquals(17, parser.parse(object));
        object.add("majorVersion", new JsonPrimitive("1.8"));
        Assertions.assertEquals(8, parser.parse(object));
        object.add("majorVersion", new JsonPrimitive(1.8));
        Assertions.assertEquals(8, parser.parse(object));
    }

}

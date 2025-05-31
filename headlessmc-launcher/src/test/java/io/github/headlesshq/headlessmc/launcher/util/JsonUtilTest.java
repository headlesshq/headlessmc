package io.github.headlesshq.headlessmc.launcher.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.github.headlesshq.headlessmc.api.util.AbstractUtilityTest;
import io.github.headlesshq.headlessmc.launcher.UsesResources;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JsonUtilTest extends AbstractUtilityTest<JsonUtil>
    implements UsesResources {

    @Test
    public void testGetString() {
        val jo = getJsonObject("nested.json");
        Assertions.assertNull(JsonUtil.getString(jo, "not", "there"));
        Assertions.assertEquals("1", JsonUtil.getString(
            jo, "object", "next", "primitive"));
        val s = JsonUtil.getString(jo, "object", "next", "object", "string");
        Assertions.assertNotNull(s);
        Assertions.assertEquals("test", s);
    }

    @Test
    public void testGetObject() {
        val jo = getJsonObject("nested.json");
        Assertions.assertNull(JsonUtil.getObject(jo, "not", "there"));
        Assertions.assertNull(JsonUtil.getObject(
            jo, "object", "next", "primitive"));
        val object = JsonUtil.getObject(jo, "object", "next", "object");
        Assertions.assertNotNull(object);
        Assertions.assertEquals("test", object.get("string").getAsString());
    }

    @Test
    public void testGetArray() {
        val jo = getJsonObject("nested.json");
        Assertions.assertNull(JsonUtil.getArray(jo, "not", "there"));
        Assertions.assertNull(JsonUtil.getArray(
            jo, "object", "next", "primitive"));
        val array = JsonUtil.getArray(jo, "object", "next", "test");
        Assertions.assertNotNull(array);
        Assertions.assertEquals("test", array.get(0).getAsString());
    }

    @Test
    public void testGetElement() {
        val jo = getJsonObject("nested.json");
        Assertions.assertNull(JsonUtil.getElement(jo, "not", "there"));
        val je = JsonUtil.getElement(jo, "object", "next", "test");
        Assertions.assertTrue(je.isJsonArray());
        Assertions.assertEquals("test",
                                je.getAsJsonArray().get(0).getAsString());
    }

    @Test
    public void testToStringMap() {
        val jo = getJsonObject("map.json");
        val map = JsonUtil.toStringMap(jo);
        Assertions.assertEquals("true", map.get("first_value"));
        Assertions.assertEquals("false", map.get("second_value"));
    }

    @Test
    public void testToBoolMap() {
        val jo = getJsonObject("map.json");
        val map = JsonUtil.toBoolMap(jo);
        Assertions.assertTrue(map.get("first_value"));
        Assertions.assertFalse(map.get("second_value"));
    }

    @Test
    public void testToMap() {
        val jo = getJsonObject("map.json");
        val map = JsonUtil.toMap(jo, JsonElement::getAsBoolean);
        Assertions.assertTrue(map.get("first_value"));
        Assertions.assertFalse(map.get("second_value"));
    }

    @Test
    public void testToList() {
        val array = new JsonArray();
        array.add(0);
        array.add(1);
        array.add(2);

        val list = JsonUtil.toList(array, JsonElement::getAsInt);
        Assertions.assertEquals(0, list.get(0));
        Assertions.assertEquals(1, list.get(1));
        Assertions.assertEquals(2, list.get(2));
    }

    @Test
    public void testToArray() {
        JsonArray array = JsonUtil.toArray(null);
        Assertions.assertEquals(0, array.size());

        val actualArray = new JsonArray();
        actualArray.add("test");
        array = JsonUtil.toArray(actualArray);
        Assertions.assertEquals(actualArray, array);

        array = JsonUtil.toArray(new JsonPrimitive("test"));
        Assertions.assertEquals(1, array.size());
        Assertions.assertEquals("test", array.get(0).getAsString());
    }

}

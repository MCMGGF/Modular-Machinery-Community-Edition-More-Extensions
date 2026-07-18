package com.fushu.mmceguiext.client.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class SubGuiConfigLoaderTest {
    @Test
    public void copyJsonObjectDeepCopiesNestedValuesWithoutJsonObjectDeepCopy() {
        JsonObject source = new JsonParser().parse(
            "{"
                + "\"text\":\"before\","
                + "\"number\":12.5,"
                + "\"enabled\":true,"
                + "\"missing\":null,"
                + "\"nested\":{\"value\":\"nested-before\"},"
                + "\"items\":[{\"id\":\"first\"},2,false,null]"
                + "}"
        ).getAsJsonObject();

        JsonObject copy = SubGuiConfigLoader.copyJsonObject(source);

        assertEquals(source, copy);
        assertNotSame(source, copy);
        assertNotSame(source.get("nested"), copy.get("nested"));
        assertNotSame(source.get("items"), copy.get("items"));

        copy.get("nested").getAsJsonObject().addProperty("value", "nested-after");
        JsonArray copiedItems = copy.get("items").getAsJsonArray();
        copiedItems.get(0).getAsJsonObject().addProperty("id", "changed");

        assertEquals("nested-before", source.get("nested").getAsJsonObject().get("value").getAsString());
        assertEquals("first", source.get("items").getAsJsonArray().get(0).getAsJsonObject().get("id").getAsString());
    }
}

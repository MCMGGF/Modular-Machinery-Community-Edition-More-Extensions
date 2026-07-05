package com.fushu.mmceguiext.common.registry;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CustomHatchRegistryTest {
    @Test
    public void loadParsesCharacterSpacingForCustomHatchTextComponents() throws Exception {
        Path file = Files.createTempFile("mmcege-custom-hatch-spacing", ".json");
        Files.write(
            file,
            ("{\n" +
                "  \"id\": \"spacing_hatch\",\n" +
                "  \"defaultCharSpacing\": 1.25,\n" +
                "  \"texts\": [\n" +
                "    {\"x\": 1, \"y\": 2, \"value\": \"Top\", \"charSpacing\": -1.0}\n" +
                "  ],\n" +
                "  \"gui\": {\n" +
                "    \"defaultLetterSpacing\": 2.5,\n" +
                "    \"components\": [\n" +
                "      {\"type\": \"text\", \"x\": 3, \"y\": 4, \"value\": \"Component\", \"letter_spacing\": 4.5}\n" +
                "    ]\n" +
                "  }\n" +
                "}").getBytes(StandardCharsets.UTF_8)
        );

        CustomHatchRegistry.CustomHatchDef def = CustomHatchRegistry.load(file);

        assertNotNull(def);
        assertEquals(Float.valueOf(1.25F), def.defaultCharSpacing);
        assertEquals(Float.valueOf(-1.0F), def.texts.get(0).charSpacing);
        assertEquals(Float.valueOf(2.5F), def.gui.defaultCharSpacing);
        assertEquals(Float.valueOf(4.5F), def.gui.components.get(0).charSpacing);
    }
}

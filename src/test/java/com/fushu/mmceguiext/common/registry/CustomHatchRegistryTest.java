package com.fushu.mmceguiext.common.registry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CustomHatchRegistryTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void slotGridPropagatesHorizontalScrollAliasesToExpandedSlots() throws Exception {
        File file = this.temporaryFolder.newFile("horizontal_slots.json");
        Files.write(file.toPath(), (
            "{\n" +
                "  \"id\": \"demo:horizontal_slots\",\n" +
                "  \"displayName\": \"Horizontal Slots\",\n" +
                "  \"componentType\": \"item\",\n" +
                "  \"ioType\": \"input\",\n" +
                "  \"gui\": {\n" +
                "    \"components\": [\n" +
                "      {\n" +
                "        \"type\": \"slots\",\n" +
                "        \"role\": \"input\",\n" +
                "        \"x\": 10,\n" +
                "        \"y\": 20,\n" +
                "        \"rows\": 3,\n" +
                "        \"columns\": 9,\n" +
                "        \"visible_columns\": 4,\n" +
                "        \"scroll_axis\": \"x\",\n" +
                "        \"scroll_mode\": \"page\",\n" +
                "        \"scrollbar_x\": 10,\n" +
                "        \"scrollbar_y\": 76,\n" +
                "        \"scrollbar_length\": 76,\n" +
                "        \"scrollbar_thumb_width\": 22,\n" +
                "        \"scrollbar_texture\": \"demo:textures/gui/scroll.png\",\n" +
                "        \"scrollbar_pressed_texture\": \"demo:textures/gui/scroll_pressed.png\",\n" +
                "        \"scrollbar_texture_width\": 64,\n" +
                "        \"scrollbar_texture_height\": 16,\n" +
                "        \"scrollbar_pressed_u\": 22,\n" +
                "        \"scrollbar_pressed_v\": 1\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}\n"
        ).getBytes(StandardCharsets.UTF_8));

        CustomHatchRegistry.CustomHatchDef def = CustomHatchRegistry.load(file.toPath());

        assertNotNull(def);
        assertNotNull(def.gui);
        assertEquals(27, def.gui.components.size());
        CustomHatchRegistry.ComponentDef first = def.gui.components.get(0);
        assertEquals("slot", first.type);
        assertEquals("input", first.role);
        assertEquals(3, first.rows);
        assertEquals(9, first.columns);
        assertEquals(4, first.visibleColumns);
        assertEquals("horizontal", first.scrollAxis);
        assertEquals("page", first.scrollMode);
        assertEquals(10, first.scrollbarX);
        assertEquals(76, first.scrollbarY);
        assertEquals(76, first.scrollbarLength);
        assertEquals(22, first.scrollbarThumbWidth);
        assertEquals("demo:textures/gui/scroll.png", first.scrollbarTexture);
        assertEquals("demo:textures/gui/scroll_pressed.png", first.scrollbarPressedTexture);
        assertEquals(64, first.scrollbarTextureWidth);
        assertEquals(16, first.scrollbarTextureHeight);
        assertEquals(22, first.scrollbarPressedU);
        assertEquals(1, first.scrollbarPressedV);
    }
}

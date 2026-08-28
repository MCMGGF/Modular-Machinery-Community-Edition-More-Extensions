package com.fushu.mmceguiext.common.registry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CustomAEMixedInputBusRegistryTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void legacySparseSlotAndTankArraysKeepOriginalIndexes() throws Exception {
        File file = this.temporaryFolder.newFile("sparse_mixed_input.json");
        Files.write(file.toPath(), (
            "{\n" +
                "  \"id\": \"demo:sparse_mixed_input\",\n" +
                "  \"configSlots\": [\n" +
                "    {\"x\": 8, \"y\": 18},\n" +
                "    null,\n" +
                "    {\"x\": 44, \"y\": 18}\n" +
                "  ],\n" +
                "  \"storageSlots\": [\n" +
                "    {\"x\": 8, \"y\": 54},\n" +
                "    \"invalid\",\n" +
                "    {\"x\": 44, \"y\": 54}\n" +
                "  ],\n" +
                "  \"fluidStorageTanks\": [\n" +
                "    {\"x\": 80, \"y\": 20, \"width\": 18, \"height\": 60},\n" +
                "    false,\n" +
                "    {\"x\": 104, \"y\": 20, \"width\": 18, \"height\": 60}\n" +
                "  ]\n" +
                "}\n"
        ).getBytes(StandardCharsets.UTF_8));

        CustomAEMixedInputBusRegistry.Def def =
            CustomAEMixedInputBusRegistry.load(file.toPath());

        assertNotNull(def);
        assertEquals(3, def.configSlots.size());
        assertEquals(8, def.configSlots.get(0).x);
        assertNull(def.configSlots.get(1));
        assertEquals(44, def.configSlots.get(2).x);

        assertEquals(3, def.storageSlots.size());
        assertNull(def.storageSlots.get(1));
        assertEquals(44, def.storageSlots.get(2).x);

        assertEquals(3, def.fluidStorageTanks.size());
        assertNull(def.fluidStorageTanks.get(1));
        assertEquals(104, def.fluidStorageTanks.get(2).x);
        assertEquals(2, countComponents(def, "tank", "fluid_storage"));
    }

    @Test
    public void duplicateComponentRoleAndIndexKeepsTheFirstMapping() throws Exception {
        File file = this.temporaryFolder.newFile("duplicate_mixed_input.json");
        Files.write(file.toPath(), (
            "{\n" +
                "  \"id\": \"demo:duplicate_mixed_input\",\n" +
                "  \"gui\": {\n" +
                "    \"components\": [\n" +
                "      {\"type\": \"slot\", \"role\": \"item_config\", \"index\": 0, \"x\": 8, \"y\": 18},\n" +
                "      {\"type\": \"slot\", \"role\": \"item_config\", \"index\": 0, \"x\": 26, \"y\": 18}\n" +
                "    ]\n" +
                "  }\n" +
                "}\n"
        ).getBytes(StandardCharsets.UTF_8));

        CustomAEMixedInputBusRegistry.Def def =
            CustomAEMixedInputBusRegistry.load(file.toPath());

        assertNotNull(def);
        assertEquals(1, def.configSlots.size());
        assertEquals(8, def.configSlots.get(0).x);
        assertEquals(18, def.configSlots.get(0).y);
    }

    private static int countComponents(
        CustomAEMixedInputBusRegistry.Def def,
        String type,
        String role
    ) {
        int count = 0;
        for (CustomAEMixedInputBusRegistry.ComponentDef component : def.gui.components) {
            if (component != null && type.equals(component.type) && role.equals(component.role)) {
                count++;
            }
        }
        return count;
    }
}

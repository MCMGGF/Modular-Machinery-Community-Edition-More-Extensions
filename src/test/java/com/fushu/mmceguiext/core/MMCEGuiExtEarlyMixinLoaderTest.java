package com.fushu.mmceguiext.core;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MMCEGuiExtEarlyMixinLoaderTest {
    @Test
    public void optionalThirdPartyMixinsAreNotRegisteredFromTheEarlyLoader() {
        assertEquals(
            Arrays.asList("mixins.mmceguiext.json"),
            MMCEGuiExtEarlyMixinLoader.ALWAYS_REGISTERED_MIXIN_CONFIGS
        );
    }

    @Test
    public void longFluidGasRequirementMixinsAreDisabledWhenConfigIsMissingOrFalse() {
        assertFalse(MMCEGuiExtEarlyMixinLoader.isLongFluidGasRequirementsEnabled(Arrays.asList(
            "general {",
            "    B:enabled=true",
            "}"
        )));
        assertFalse(MMCEGuiExtEarlyMixinLoader.isLongFluidGasRequirementsEnabled(Arrays.asList(
            "experimental {",
            "    B:enableLongFluidGasRequirements=false",
            "}"
        )));
    }

    @Test
    public void longFluidGasRequirementMixinsAreEnabledOnlyByExplicitTrueValue() {
        assertTrue(MMCEGuiExtEarlyMixinLoader.isLongFluidGasRequirementsEnabled(Arrays.asList(
            "experimental {",
            "    B:enableLongFluidGasRequirements=true",
            "}"
        )));
        assertTrue(MMCEGuiExtEarlyMixinLoader.isLongFluidGasRequirementsEnabled(Arrays.asList(
            "experimental {",
            "    enableLongFluidGasRequirements = TRUE # manual shorthand is accepted",
            "}"
        )));
    }

    @Test
    public void customParallelControllerTiersRequireExplicitOptIn() {
        assertFalse(MMCEGuiExtEarlyMixinLoader.isCustomParallelControllerTiersEnabled(Arrays.asList(
            "experimental {",
            "    B:enableCustomParallelControllerTiers=false",
            "}"
        )));
        assertTrue(MMCEGuiExtEarlyMixinLoader.isCustomParallelControllerTiersEnabled(Arrays.asList(
            "experimental {",
            "    B:enableCustomParallelControllerTiers=true",
            "    I:customParallelControllerTierCount=2",
            "    I:customParallelControllerDefaultMaxParallelism=64",
            "    S:customParallelControllerMaxParallelisms <",
            "        16",
            "        128",
            "    >",
            "}"
        )));
        assertEquals(2, MMCEGuiExtEarlyMixinLoader.getCustomParallelControllerTierCount());
        assertEquals(64, MMCEGuiExtEarlyMixinLoader.getCustomParallelControllerDefaultMaxParallelism());
        assertEquals(16, MMCEGuiExtEarlyMixinLoader.getCustomParallelControllerMaxParallelism(0));
        assertEquals(128, MMCEGuiExtEarlyMixinLoader.getCustomParallelControllerMaxParallelism(1));
        assertEquals(64, MMCEGuiExtEarlyMixinLoader.getCustomParallelControllerMaxParallelism(2));
    }

    @Test
    public void customParallelControllerSettingsAreClamped() {
        assertTrue(MMCEGuiExtEarlyMixinLoader.isCustomParallelControllerTiersEnabled(Arrays.asList(
            "B:enableCustomParallelControllerTiers=true",
            "I:customParallelControllerTierCount=100",
            "I:customParallelControllerDefaultMaxParallelism=0"
        )));
        assertEquals(16, MMCEGuiExtEarlyMixinLoader.getCustomParallelControllerTierCount());
        assertEquals(1, MMCEGuiExtEarlyMixinLoader.getCustomParallelControllerDefaultMaxParallelism());
    }

    @Test
    public void perTierParallelismAcceptsCompactValuesAndFallsBackWhenMissing() {
        assertTrue(MMCEGuiExtEarlyMixinLoader.isCustomParallelControllerTiersEnabled(Arrays.asList(
            "B:enableCustomParallelControllerTiers=true",
            "I:customParallelControllerTierCount=3",
            "I:customParallelControllerDefaultMaxParallelism=32",
            "S:customParallelControllerMaxParallelisms=8, 16, 256"
        )));
        assertEquals(8, MMCEGuiExtEarlyMixinLoader.getCustomParallelControllerMaxParallelism(0));
        assertEquals(16, MMCEGuiExtEarlyMixinLoader.getCustomParallelControllerMaxParallelism(1));
        assertEquals(256, MMCEGuiExtEarlyMixinLoader.getCustomParallelControllerMaxParallelism(2));
        assertEquals(32, MMCEGuiExtEarlyMixinLoader.getCustomParallelControllerMaxParallelism(3));
    }

    @Test
    public void whimCraftParallelControllerSettingAllowsMmceMeWhenExplicitlyDisabled() {
        assertFalse(MMCEGuiExtEarlyMixinLoader.readWhimCraftParallelControllerSetting(Arrays.asList(
            "mmce_config {",
            "    I:otherParallelController=0",
            "}"
        )));
        assertTrue(MMCEGuiExtEarlyMixinLoader.readWhimCraftParallelControllerSetting(Arrays.asList(
            "I:otherParallelController=1"
        )));
    }

    @Test
    public void missingOrMalformedWhimCraftParallelControllerSettingKeepsConflictGuard() {
        assertTrue(MMCEGuiExtEarlyMixinLoader.readWhimCraftParallelControllerSetting(Arrays.asList(
            "mmce_config {",
            "    B:isIgnoreParallel=false",
            "}"
        )));
        assertTrue(MMCEGuiExtEarlyMixinLoader.readWhimCraftParallelControllerSetting(Arrays.asList(
            "I:otherParallelController=not-a-number"
        )));
    }

    @Test
    public void whimCraftConfigIsReadFromTheMinecraftDirectory() throws Exception {
        Path minecraftDirectory = Files.createTempDirectory("mmceguiext-whimcraft");
        try {
            Files.createDirectories(minecraftDirectory.resolve("mods"));
            Files.createDirectories(minecraftDirectory.resolve("config"));
            Files.createDirectories(minecraftDirectory.resolve("config/mmceguiext"));
            Files.write(
                minecraftDirectory.resolve("mods/WhimCraft-0.1.4.jar"),
                new byte[]{0}
            );
            Files.write(
                minecraftDirectory.resolve("config/WhimCraft.cfg"),
                Arrays.asList(
                    "general {",
                    "    mmce_config {",
                    "        I:otherParallelController=0",
                    "    }",
                    "}"
                ),
                StandardCharsets.UTF_8
            );

            assertFalse(MMCEGuiExtEarlyMixinLoader.isWhimCraftParallelExtensionEnabled(
                minecraftDirectory.toFile()
            ));
        } finally {
            deleteRecursively(minecraftDirectory.toFile());
        }
    }

    @Test
    public void disabledWhimCraftParallelTiersAllowMmceMeThroughTheStartupGate() throws Exception {
        Path minecraftDirectory = Files.createTempDirectory("mmceguiext-whimcraft-gate");
        try {
            Files.createDirectories(minecraftDirectory.resolve("mods"));
            Files.createDirectories(minecraftDirectory.resolve("config"));
            Files.createDirectories(minecraftDirectory.resolve("config/mmceguiext"));
            Files.write(
                minecraftDirectory.resolve("mods/WhimCraft-0.1.4.jar"),
                new byte[]{0}
            );
            Files.write(
                minecraftDirectory.resolve("config/WhimCraft.cfg"),
                Arrays.asList(
                    "general {",
                    "    mmce_config {",
                    "        I:otherParallelController=0",
                    "    }",
                    "}"
                ),
                StandardCharsets.UTF_8
            );
            Files.write(
                minecraftDirectory.resolve("config/mmceguiext/client.cfg"),
                Arrays.asList(
                    "experimental {",
                    "    B:enableCustomParallelControllerTiers=true",
                    "    I:customParallelControllerTierCount=2",
                    "}"
                ),
                StandardCharsets.UTF_8
            );

            java.util.Map<String, Object> data = new java.util.HashMap<String, Object>();
            data.put("mcLocation", minecraftDirectory.toFile());
            assertTrue(MMCEGuiExtEarlyMixinLoader.isCustomParallelControllerTiersEnabled(data));
            assertTrue(MMCEGuiExtEarlyMixinLoader.areCustomParallelControllerTiersEnabled());
            assertEquals(2, MMCEGuiExtEarlyMixinLoader.getCustomParallelControllerTierCount());
        } finally {
            deleteRecursively(minecraftDirectory.toFile());
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

}

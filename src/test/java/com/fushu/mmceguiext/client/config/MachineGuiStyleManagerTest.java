package com.fushu.mmceguiext.client.config;

import com.fushu.mmceguiext.MMCEGuiExtConfig;
import net.minecraft.util.ResourceLocation;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class MachineGuiStyleManagerTest {

    @Test
    public void externalMachineStyleRegistrationReplacesPreviousValue() throws Exception {
        MachineGuiStyleManager.clearExternalStyles();
        ResourceLocation machine = new ResourceLocation("mmceoneblock", "starter_controller");

        MachineGuiStyleManager.ControllerStyle first = styleWithText("first");
        MachineGuiStyleManager.ControllerStyle second = styleWithText("second");

        MachineGuiStyleManager.registerExternalMachineControllerStyle(machine, first);
        MachineGuiStyleManager.registerExternalMachineControllerStyle(machine, second);

        MachineGuiStyleManager.ControllerStyle registered = externalMachineStyles().get("mmceoneblock:starter_controller");

        assertEquals(1, registered.texts.size());
        assertEquals("second", registered.texts.get(0).value);
        assertTrue(externalMachineStyles().containsKey("starter_controller"));
        assertEquals(1, externalMachineStyles().get("starter_controller").texts.size());
    }

    @Test
    public void managerResolvesExternalMachineStyleByResourceLocation() {
        MachineGuiStyleManager.clearExternalStyles();
        ResourceLocation styleKey = new ResourceLocation("mmceoneblock", "starter_controller");

        MachineGuiStyleManager.ControllerStyle registered = styleWithText("from-api");
        MachineGuiStyleManager.registerExternalMachineControllerStyle(styleKey, registered);

        MachineGuiStyleManager.ControllerStyle resolved = MachineGuiStyleManager.resolveMachineController(styleKey);

        assertEquals(1, resolved.texts.size());
        assertEquals("from-api", resolved.texts.get(0).value);
    }

    @Test
    public void managerResolveReturnsDefensiveCopy() {
        MachineGuiStyleManager.clearExternalStyles();
        ResourceLocation styleKey = new ResourceLocation("mmceoneblock", "starter_controller");

        MachineGuiStyleManager.ControllerStyle registered = styleWithText("cached");
        MachineGuiStyleManager.registerExternalMachineControllerStyle(styleKey, registered);

        MachineGuiStyleManager.ControllerStyle first = MachineGuiStyleManager.resolveMachineController(styleKey);
        first.texts.get(0).value = "mutated";
        first.texts.add(text("external"));

        MachineGuiStyleManager.ControllerStyle second = MachineGuiStyleManager.resolveMachineController(styleKey);

        assertNotSame(first, second);
        assertEquals(1, second.texts.size());
        assertEquals("cached", second.texts.get(0).value);
    }

    @Test
    public void oversizedMachineGuiJsonUsesConfiguredLimit() throws Exception {
        int previousLimit = MMCEGuiExtConfig.maxGuiConfigFileSizeMiB;
        Map<String, MachineGuiStyleManager.ControllerStyle> styles = machineControllerStyles();
        styles.clear();
        Path allowed = Files.createTempFile("mmcege-large-machine-allowed", ".json");
        Path rejected = Files.createTempFile("mmcege-large-machine-rejected", ".json");
        try {
            Files.write(allowed, largeMachineJson("demo:large_allowed", 1_048_600).getBytes(StandardCharsets.UTF_8));
            MMCEGuiExtConfig.maxGuiConfigFileSizeMiB = 8;
            invokeLoadMachineJson(allowed);
            assertTrue(styles.containsKey("demo:large_allowed"));

            Files.write(rejected, largeMachineJson("demo:large_rejected", 1_048_600).getBytes(StandardCharsets.UTF_8));
            MMCEGuiExtConfig.maxGuiConfigFileSizeMiB = 1;
            invokeLoadMachineJson(rejected);
            assertFalse(styles.containsKey("demo:large_rejected"));
        } finally {
            MMCEGuiExtConfig.maxGuiConfigFileSizeMiB = previousLimit;
            styles.clear();
            Files.deleteIfExists(allowed);
            Files.deleteIfExists(rejected);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, MachineGuiStyleManager.ControllerStyle> externalMachineStyles() throws Exception {
        Field field = MachineGuiStyleManager.class.getDeclaredField("EXTERNAL_MACHINE_CONTROLLER_STYLES");
        field.setAccessible(true);
        return (Map<String, MachineGuiStyleManager.ControllerStyle>) field.get(null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, MachineGuiStyleManager.ControllerStyle> machineControllerStyles() throws Exception {
        Field field = MachineGuiStyleManager.class.getDeclaredField("MACHINE_CONTROLLER_STYLES");
        field.setAccessible(true);
        return (Map<String, MachineGuiStyleManager.ControllerStyle>) field.get(null);
    }

    private static void invokeLoadMachineJson(Path path) throws Exception {
        Method method = MachineGuiStyleManager.class.getDeclaredMethod("loadMachineJson", Path.class);
        method.setAccessible(true);
        method.invoke(null, path);
    }

    private static String largeMachineJson(String registryName, int paddingLength) {
        StringBuilder padding = new StringBuilder(paddingLength);
        while (padding.length() < paddingLength) {
            padding.append('x');
        }
        return "{\"registryname\":\"" + registryName + "\","
            + "\"mmce_gui_ext\":{\"machineController\":{\"texts\":[]}},"
            + "\"padding\":\"" + padding + "\"}";
    }

    private static MachineGuiStyleManager.ControllerStyle styleWithText(String value) {
        MachineGuiStyleManager.ControllerStyle style = new MachineGuiStyleManager.ControllerStyle();
        style.texts = new ArrayList<MachineGuiStyleManager.TextStyle>();
        style.texts.add(text(value));
        return style;
    }

    private static MachineGuiStyleManager.TextStyle text(String value) {
        MachineGuiStyleManager.TextStyle text = new MachineGuiStyleManager.TextStyle();
        text.value = value;
        return text;
    }
}

package com.fushu.mmceguiext.client.config;

import com.fushu.mmceguiext.api.gui.MachineGuiStyleApi;
import net.minecraft.util.ResourceLocation;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.assertEquals;
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
    public void publicApiResolvesExternalMachineStyleByResourceLocation() {
        MachineGuiStyleManager.clearExternalStyles();
        ResourceLocation styleKey = new ResourceLocation("mmceoneblock", "starter_controller");

        MachineGuiStyleManager.ControllerStyle registered = styleWithText("from-api");
        MachineGuiStyleApi.registerMachineControllerStyle(styleKey, registered);

        MachineGuiStyleManager.ControllerStyle resolved = MachineGuiStyleApi.resolveMachineControllerStyle(styleKey);

        assertEquals(1, resolved.texts.size());
        assertEquals("from-api", resolved.texts.get(0).value);
    }

    @Test
    public void publicApiResolveReturnsDefensiveCopy() {
        MachineGuiStyleManager.clearExternalStyles();
        ResourceLocation styleKey = new ResourceLocation("mmceoneblock", "starter_controller");

        MachineGuiStyleManager.ControllerStyle registered = styleWithText("cached");
        MachineGuiStyleApi.registerMachineControllerStyle(styleKey, registered);

        MachineGuiStyleManager.ControllerStyle first = MachineGuiStyleApi.resolveMachineControllerStyle(styleKey);
        first.texts.get(0).value = "mutated";
        first.texts.add(text("external"));

        MachineGuiStyleManager.ControllerStyle second = MachineGuiStyleApi.resolveMachineControllerStyle(styleKey);

        assertNotSame(first, second);
        assertEquals(1, second.texts.size());
        assertEquals("cached", second.texts.get(0).value);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, MachineGuiStyleManager.ControllerStyle> externalMachineStyles() throws Exception {
        Field field = MachineGuiStyleManager.class.getDeclaredField("EXTERNAL_MACHINE_CONTROLLER_STYLES");
        field.setAccessible(true);
        return (Map<String, MachineGuiStyleManager.ControllerStyle>) field.get(null);
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

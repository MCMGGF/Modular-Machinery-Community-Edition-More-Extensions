package com.fushu.mmceguiext.api;

import com.fushu.mmceguiext.MMCEGuiExtApi;
import com.fushu.mmceguiext.api.gui.MachineGuiBridge;
import hellfirepvp.modularmachinery.common.container.ContainerController;
import hellfirepvp.modularmachinery.common.container.ContainerFactoryController;
import net.minecraft.client.gui.GuiScreen;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MMCEGuiExtApiTest {
    @Test
    public void exposesStableApiLevel() {
        assertEquals(1, MMCEGuiExtApi.API_LEVEL);
        assertTrue(MMCEGuiExtApi.isApiLevelAtLeast(1));
        assertFalse(MMCEGuiExtApi.isApiLevelAtLeast(2));
        assertFalse(MMCEGuiExtApi.isApiLevelAtLeast(-1));
    }

    @Test
    public void guiBridgeDoesNotExposeResizableImplementationTypes() throws Exception {
        Method machine = MachineGuiBridge.class.getMethod(
            "createMachineControllerScreen",
            ContainerController.class
        );
        Method factory = MachineGuiBridge.class.getMethod(
            "createFactoryControllerScreen",
            ContainerFactoryController.class
        );

        assertEquals(GuiScreen.class, machine.getReturnType());
        assertEquals(GuiScreen.class, factory.getReturnType());
    }
}

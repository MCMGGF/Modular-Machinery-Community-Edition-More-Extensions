package com.fushu.mmceguiext.common.network;

import com.fushu.mmceguiext.common.util.ControllerSmartInterfaceAccess;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PktControllerSmartInterfaceUpdateTest {
    @Test
    public void controllerAccessContractIsPreferredOverReflectiveFallback() throws Exception {
        AccessController controller = new AccessController();
        Method method = PktControllerSmartInterfaceUpdate.class.getDeclaredMethod(
            "tryInvokeControllerSmartUpdate",
            hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController.class,
            String.class,
            float.class
        );
        method.setAccessible(true);

        Object result = method.invoke(null, controller, "food", 42.0F);

        assertEquals(Boolean.TRUE, result);
        assertTrue(controller.called);
        assertEquals("food", controller.interfaceType);
        assertEquals(42.0F, controller.value, 0.0001F);
    }

    private static final class AccessController extends TileMachineController
        implements ControllerSmartInterfaceAccess {

        private boolean called;
        private String interfaceType;
        private float value;

        @Override
        public boolean mmceguiext$updateSmartInterfaceValue(String interfaceType, float value) {
            this.called = true;
            this.interfaceType = interfaceType;
            this.value = value;
            return true;
        }
    }
}

package com.fushu.mmceguiext.common.network;

import com.fushu.mmceguiext.common.util.ControllerSmartInterfaceAccess;
import com.fushu.mmceguiext.common.util.ControllerCustomDataAccess;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftingStatus;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
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
            Object.class,
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

    @Test
    public void successfulControllerAccessIsMirroredToCustomData() {
        MirroringController controller = new MirroringController();

        assertTrue(PktControllerSmartInterfaceUpdate.tryInvokeAndMirrorControllerSmartUpdate(
            controller,
            "speed",
            7.5F
        ));
        assertEquals(Float.valueOf(7.5F), ControllerCustomDataAccess.readNumber(controller, "speed"));
    }

    private static final class AccessController implements ControllerSmartInterfaceAccess {

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

    private static final class MirroringController
        extends TileMultiblockMachineController
        implements ControllerSmartInterfaceAccess {

        @Override
        public void doControllerTick() {
        }

        @Override
        public CraftingStatus getControllerStatus() {
            return null;
        }

        @Override
        public void setControllerStatus(CraftingStatus status) {
        }

        @Override
        public void flushContextModifier() {
        }

        @Override
        public boolean mmceguiext$updateSmartInterfaceValue(String interfaceType, float value) {
            return true;
        }
    }
}

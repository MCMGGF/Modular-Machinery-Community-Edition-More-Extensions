package com.fushu.mmceguiext.client.gui;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GuiFluidProcessorHatchCustomTest {
    @Test
    public void handledLeftClickAndDragAreConsumedBeforeSlotRouting() {
        assertTrue(GuiFluidProcessorHatchCustom.shouldConsumeSlotGridPointerEvent(0, true));
        assertFalse(GuiFluidProcessorHatchCustom.shouldConsumeSlotGridPointerEvent(0, false));
        assertFalse(GuiFluidProcessorHatchCustom.shouldConsumeSlotGridPointerEvent(1, true));
    }
}

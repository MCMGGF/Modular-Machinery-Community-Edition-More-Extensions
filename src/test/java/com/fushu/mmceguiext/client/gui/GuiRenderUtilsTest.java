package com.fushu.mmceguiext.client.gui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GuiRenderUtilsTest {
    @Test
    public void widthHelperAddsSpacingOnlyBetweenVisibleCharacters() {
        assertEquals(14.0F, GuiRenderUtils.getStringWidthWithCharSpacing(10, "ABC", 2.0F), 0.0001F);
        assertEquals(12.0F, GuiRenderUtils.getStringWidthWithCharSpacing(10, "\u00A7aA\u00A7lB", 2.0F), 0.0001F);
    }

    @Test
    public void widthHelperPreservesNegativeSpacingAndZeroFallback() {
        assertEquals(6.0F, GuiRenderUtils.getStringWidthWithCharSpacing(10, "ABC", -2.0F), 0.0001F);
        assertEquals(10.0F, GuiRenderUtils.getStringWidthWithCharSpacing(10, "ABC", 0.0F), 0.0001F);
        assertEquals(10.0F, GuiRenderUtils.getStringWidthWithCharSpacing(10, "A", 100.0F), 0.0001F);
    }

    @Test
    public void visibleCharacterCounterIgnoresMinecraftFormattingCodes() {
        assertEquals(2, GuiRenderUtils.countVisibleTextChars("\u00A7aA\u00A7lB"));
        assertEquals(3, GuiRenderUtils.countVisibleTextChars("A B"));
    }

    @Test
    public void guiCoordinateHelperOffsetsOnlyScreenSpaceRendering() {
        assertEquals(12, GuiRenderUtils.resolveGuiCoordinate(12, 100, false));
        assertEquals(112, GuiRenderUtils.resolveGuiCoordinate(12, 100, true));
        assertEquals(88, GuiRenderUtils.resolveGuiCoordinate(-12, 100, true));
    }
}

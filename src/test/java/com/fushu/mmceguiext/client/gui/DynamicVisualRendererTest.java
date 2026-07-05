package com.fushu.mmceguiext.client.gui;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class DynamicVisualRendererTest {
    @Test
    public void computeAnimatedFrameIndexHonorsTicksPerFrame() {
        assertEquals(0, DynamicVisualRenderer.computeAnimatedFrameIndex(0L, 4, 2, 0, true, false, false));
        assertEquals(0, DynamicVisualRenderer.computeAnimatedFrameIndex(1L, 4, 2, 0, true, false, false));
        assertEquals(1, DynamicVisualRenderer.computeAnimatedFrameIndex(2L, 4, 2, 0, true, false, false));
        assertEquals(2, DynamicVisualRenderer.computeAnimatedFrameIndex(4L, 4, 2, 0, true, false, false));
    }

    @Test
    public void computeAnimatedFrameIndexAppliesStartFrameAndLooping() {
        assertEquals(2, DynamicVisualRenderer.computeAnimatedFrameIndex(0L, 4, 1, 2, true, false, false));
        assertEquals(3, DynamicVisualRenderer.computeAnimatedFrameIndex(1L, 4, 1, 2, true, false, false));
        assertEquals(0, DynamicVisualRenderer.computeAnimatedFrameIndex(2L, 4, 1, 2, true, false, false));
    }

    @Test
    public void computeAnimatedFrameIndexClampsWhenLoopIsDisabled() {
        assertEquals(1, DynamicVisualRenderer.computeAnimatedFrameIndex(0L, 4, 1, 1, false, false, false));
        assertEquals(3, DynamicVisualRenderer.computeAnimatedFrameIndex(20L, 4, 1, 1, false, false, false));
    }

    @Test
    public void computeAnimatedFrameIndexCanReversePlayback() {
        assertEquals(3, DynamicVisualRenderer.computeAnimatedFrameIndex(0L, 4, 1, 0, true, true, false));
        assertEquals(2, DynamicVisualRenderer.computeAnimatedFrameIndex(1L, 4, 1, 0, true, true, false));
        assertEquals(0, DynamicVisualRenderer.computeAnimatedFrameIndex(3L, 4, 1, 0, true, true, false));
    }

    @Test
    public void computeAnimatedFrameIndexSupportsPingPongCycles() {
        assertEquals(0, DynamicVisualRenderer.computeAnimatedFrameIndex(0L, 4, 1, 0, true, false, true));
        assertEquals(1, DynamicVisualRenderer.computeAnimatedFrameIndex(1L, 4, 1, 0, true, false, true));
        assertEquals(2, DynamicVisualRenderer.computeAnimatedFrameIndex(2L, 4, 1, 0, true, false, true));
        assertEquals(3, DynamicVisualRenderer.computeAnimatedFrameIndex(3L, 4, 1, 0, true, false, true));
        assertEquals(2, DynamicVisualRenderer.computeAnimatedFrameIndex(4L, 4, 1, 0, true, false, true));
        assertEquals(1, DynamicVisualRenderer.computeAnimatedFrameIndex(5L, 4, 1, 0, true, false, true));
        assertEquals(0, DynamicVisualRenderer.computeAnimatedFrameIndex(6L, 4, 1, 0, true, false, true));
    }

    @Test
    public void computeSpriteSheetFrameUvUsesColumnsAndBaseUv() {
        assertArrayEquals(new int[] {2, 3}, DynamicVisualRenderer.computeSpriteSheetFrameUv(0, 2, 3, 16, 18, 4));
        assertArrayEquals(new int[] {50, 3}, DynamicVisualRenderer.computeSpriteSheetFrameUv(3, 2, 3, 16, 18, 4));
        assertArrayEquals(new int[] {18, 21}, DynamicVisualRenderer.computeSpriteSheetFrameUv(5, 2, 3, 16, 18, 4));
    }
}

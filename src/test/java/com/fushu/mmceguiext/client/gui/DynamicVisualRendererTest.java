package com.fushu.mmceguiext.client.gui;

import com.fushu.mmceguiext.client.config.MachineGuiStyleManager;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    public void computeAnimatedFrameIndexClampsNonLoopingPingPongAtTheFirstFrame() {
        assertEquals(3, DynamicVisualRenderer.computeAnimatedFrameIndex(3L, 4, 1, 0, false, false, true));
        assertEquals(2, DynamicVisualRenderer.computeAnimatedFrameIndex(4L, 4, 1, 0, false, false, true));
        assertEquals(0, DynamicVisualRenderer.computeAnimatedFrameIndex(6L, 4, 1, 0, false, false, true));
        assertEquals(0, DynamicVisualRenderer.computeAnimatedFrameIndex(Long.MAX_VALUE, 4, 1, 0, false, false, true));
    }

    @Test
    public void computeAnimatedFrameIndexHandlesNegativeAndHugeTicksSafely() {
        assertEquals(0, DynamicVisualRenderer.computeAnimatedFrameIndex(-100L, 4, 2, 0, true, false, false));
        assertEquals(
            1,
            DynamicVisualRenderer.computeAnimatedFrameIndex(Long.MAX_VALUE, 4, Integer.MAX_VALUE, 0, true, true, false)
        );
    }

    @Test
    public void computeSpriteSheetFrameUvUsesColumnsAndBaseUv() {
        assertArrayEquals(new int[] {2, 3}, DynamicVisualRenderer.computeSpriteSheetFrameUv(0, 2, 3, 16, 18, 4));
        assertArrayEquals(new int[] {50, 3}, DynamicVisualRenderer.computeSpriteSheetFrameUv(3, 2, 3, 16, 18, 4));
        assertArrayEquals(new int[] {18, 21}, DynamicVisualRenderer.computeSpriteSheetFrameUv(5, 2, 3, 16, 18, 4));
    }

    @Test
    public void textureSwitchFrameOverridesRendererTextureCoordinates() {
        MachineGuiStyleManager.DynamicVisualRendererStyle renderer =
            new MachineGuiStyleManager.DynamicVisualRendererStyle();
        renderer.u = Integer.valueOf(2);
        renderer.v = Integer.valueOf(3);
        renderer.textureWidth = Integer.valueOf(64);
        renderer.textureHeight = Integer.valueOf(64);

        MachineGuiStyleManager.DynamicVisualFrameStyle frame =
            new MachineGuiStyleManager.DynamicVisualFrameStyle();
        frame.u = Integer.valueOf(8);
        frame.v = Integer.valueOf(9);
        frame.textureWidth = Integer.valueOf(32);
        frame.textureHeight = Integer.valueOf(16);

        assertArrayEquals(
            new int[] {8, 9, 32, 16},
            DynamicVisualRenderer.resolveTextureSwitchDrawSpec(renderer, frame, 16, 16)
        );
    }

    @Test
    public void textureSwitchFrameFallsBackToRendererCoordinatesWhenUnset() {
        MachineGuiStyleManager.DynamicVisualRendererStyle renderer =
            new MachineGuiStyleManager.DynamicVisualRendererStyle();
        renderer.u = Integer.valueOf(2);
        renderer.v = Integer.valueOf(3);
        renderer.textureWidth = Integer.valueOf(64);
        renderer.textureHeight = Integer.valueOf(64);

        assertArrayEquals(
            new int[] {2, 3, 64, 64},
            DynamicVisualRenderer.resolveTextureSwitchDrawSpec(renderer, null, 16, 16)
        );
    }

    @Test
    public void historySamplingRejectsTimeRegressionAndRequiresInterval() {
        assertTrue(DynamicVisualRenderer.shouldSampleHistory(Long.MIN_VALUE, 20L, 5, true));
        assertTrue(DynamicVisualRenderer.shouldSampleHistory(10L, 5L, 5, false));
        assertTrue(DynamicVisualRenderer.shouldSampleHistory(10L, 15L, 5, false));
        assertFalse(DynamicVisualRenderer.shouldSampleHistory(10L, 14L, 5, false));
    }

    @Test
    public void weightedSourcesHandleZeroAndNegativeWeights() {
        DynamicVisualRenderer renderer = new DynamicVisualRenderer();
        MachineGuiStyleManager.DynamicVisualSourceStyle weighted =
            new MachineGuiStyleManager.DynamicVisualSourceStyle();
        weighted.combine = "weightedAverage";
        weighted.sources = java.util.Arrays.asList(
            child("a", 10.0F, 0.0F),
            child("b", 20.0F, -1.0F),
            child("c", 30.0F, 2.0F)
        );

        double result = renderer.combineSourceValuesNumber(weighted, null, null, -1.0D);
        assertEquals((20.0D * -1.0D + 30.0D * 2.0D) / 1.0D, result, 0.0001D);
    }

    @Test
    public void weightedAverageWithZeroTotalWeightFallsBack() {
        DynamicVisualRenderer renderer = new DynamicVisualRenderer();
        MachineGuiStyleManager.DynamicVisualSourceStyle weighted =
            new MachineGuiStyleManager.DynamicVisualSourceStyle();
        weighted.combine = "weightedAverage";
        weighted.sources = java.util.Arrays.asList(
            child("a", 10.0F, 1.0F),
            child("b", 20.0F, -1.0F)
        );

        assertEquals(-7.0D, renderer.combineSourceValuesNumber(weighted, null, null, -7.0D), 0.0001D);
    }

    @Test
    public void weightedSourceOverflowFallsBackInsteadOfReturningInfinity() {
        DynamicVisualRenderer renderer = new DynamicVisualRenderer();
        MachineGuiStyleManager.DynamicVisualSourceStyle weighted =
            new MachineGuiStyleManager.DynamicVisualSourceStyle();
        weighted.combine = "weightedSum";
        weighted.sources = java.util.Arrays.asList(
            machineChild("a", Float.MAX_VALUE),
            machineChild("b", Float.MAX_VALUE)
        );

        DynamicVisualRenderer.MetricProvider provider = new DynamicVisualRenderer.MetricProvider() {
            @Override
            public float getMachineMetric(String metric, float fallback) {
                return Float.MAX_VALUE;
            }

            @Override
            public double getMachineMetricValue(String metric, double fallback) {
                return Double.MAX_VALUE;
            }
        };
        assertEquals(-3.0D, renderer.combineSourceValuesNumber(weighted, null, provider, -3.0D), 0.0001D);
    }

    private static MachineGuiStyleManager.DynamicVisualSourceStyle child(String key, float value, float weight) {
        MachineGuiStyleManager.DynamicVisualSourceStyle child =
            new MachineGuiStyleManager.DynamicVisualSourceStyle();
        child.type = "customData";
        child.key = key;
        child.defaultValue = Float.valueOf(value);
        child.weight = Float.valueOf(weight);
        return child;
    }

    private static MachineGuiStyleManager.DynamicVisualSourceStyle machineChild(String metric, float weight) {
        MachineGuiStyleManager.DynamicVisualSourceStyle child =
            new MachineGuiStyleManager.DynamicVisualSourceStyle();
        child.type = "machine";
        child.metric = metric;
        child.weight = Float.valueOf(weight);
        return child;
    }
}

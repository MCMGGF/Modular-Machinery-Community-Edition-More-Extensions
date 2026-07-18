package com.fushu.mmceguiext.client.gui;

import com.fushu.mmceguiext.client.config.MachineGuiStyleManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DynamicVisualBoundsTest {
    @Test
    public void finiteDynamicBoundsNormalizeRawValue() {
        assertEquals(
            0.5F,
            DynamicVisualRenderer.normalizeValueWithBounds(60.0F, 20.0F, 100.0F, true, false),
            0.0001F
        );
    }

    @Test
    public void maxLessThanOrEqualToMinReturnsZero() {
        assertEquals(
            0.0F,
            DynamicVisualRenderer.normalizeValueWithBounds(60.0F, 100.0F, 100.0F, true, false),
            0.0001F
        );
        assertEquals(
            0.0F,
            DynamicVisualRenderer.normalizeValueWithBounds(60.0F, 100.0F, 20.0F, true, false),
            0.0001F
        );
        assertEquals(
            0.0F,
            DynamicVisualRenderer.normalizeValueWithBounds(60.0F, 100.0F, 20.0F, true, true),
            0.0001F
        );
    }

    @Test
    public void invalidInputsDoNotProduceNonFiniteResults() {
        assertEquals(
            0.0F,
            DynamicVisualRenderer.normalizeValueWithBounds(
                Float.NaN, 0.0F, Float.POSITIVE_INFINITY, true, false
            ),
            0.0001F
        );
    }

    @Test
    public void invertKeepsExistingSemantics() {
        assertEquals(
            0.75F,
            DynamicVisualRenderer.normalizeValueWithBounds(25.0F, 0.0F, 100.0F, true, true),
            0.0001F
        );
    }

    @Test
    public void missingBoundValueFallsBackToStaticBound() {
        MachineGuiStyleManager.DynamicVisualSourceStyle source =
            new MachineGuiStyleManager.DynamicVisualSourceStyle();
        source.type = "customData";
        source.key = "missing_capacity";

        assertEquals(
            3200.0F,
            new DynamicVisualRenderer().resolveDynamicBound(source, 3200.0F, null, null),
            0.0001F
        );
    }

    @Test
    public void explicitBoundDefaultOverridesStaticBound() {
        MachineGuiStyleManager.DynamicVisualSourceStyle source =
            new MachineGuiStyleManager.DynamicVisualSourceStyle();
        source.type = "customData";
        source.key = "missing_capacity";
        source.defaultValue = Float.valueOf(6400.0F);

        assertEquals(
            6400.0F,
            new DynamicVisualRenderer().resolveDynamicBound(source, 3200.0F, null, null),
            0.0001F
        );
    }
}

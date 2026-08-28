package com.fushu.mmceguiext.client.gui;

import com.fushu.mmceguiext.client.config.MachineGuiStyleManager;
import com.fushu.mmceguiext.common.util.ControllerCustomDataAccess;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
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
    public void longDynamicBoundsKeepRatioBeyondFloatRange() {
        assertEquals(
            0.5F,
            DynamicVisualRenderer.normalizeValueWithDoubleBounds(
                3_000_000_000L,
                0L,
                6_000_000_000L,
                true,
                false
            ),
            0.0001F
        );
    }

    @Test
    public void customDataLongCanDriveDynamicMaximum() {
        TileMachineController controller = new TileMachineController();
        ControllerCustomDataAccess.writeLong(controller, "food", 3_000_000_000L);
        ControllerCustomDataAccess.writeLong(controller, "food_max", 6_000_000_000L);

        MachineGuiStyleManager.DynamicVisualSourceStyle source =
            new MachineGuiStyleManager.DynamicVisualSourceStyle();
        source.type = "customData";
        source.key = "food";
        source.min = Float.valueOf(0.0F);

        MachineGuiStyleManager.DynamicVisualSourceStyle maxSource =
            new MachineGuiStyleManager.DynamicVisualSourceStyle();
        maxSource.type = "customData";
        maxSource.key = "food_max";
        source.maxSource = maxSource;

        assertEquals(
            0.5F,
            new DynamicVisualRenderer().resolveNormalizedInput(source, 0.0F, controller, null),
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

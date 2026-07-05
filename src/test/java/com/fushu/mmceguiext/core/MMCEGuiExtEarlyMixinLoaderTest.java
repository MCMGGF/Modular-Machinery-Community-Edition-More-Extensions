package com.fushu.mmceguiext.core;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MMCEGuiExtEarlyMixinLoaderTest {
    @Test
    public void longFluidGasRequirementMixinsAreDisabledWhenConfigIsMissingOrFalse() {
        assertFalse(MMCEGuiExtEarlyMixinLoader.isLongFluidGasRequirementsEnabled(Arrays.asList(
            "general {",
            "    B:enabled=true",
            "}"
        )));
        assertFalse(MMCEGuiExtEarlyMixinLoader.isLongFluidGasRequirementsEnabled(Arrays.asList(
            "experimental {",
            "    B:enableLongFluidGasRequirements=false",
            "}"
        )));
    }

    @Test
    public void longFluidGasRequirementMixinsAreEnabledOnlyByExplicitTrueValue() {
        assertTrue(MMCEGuiExtEarlyMixinLoader.isLongFluidGasRequirementsEnabled(Arrays.asList(
            "experimental {",
            "    B:enableLongFluidGasRequirements=true",
            "}"
        )));
        assertTrue(MMCEGuiExtEarlyMixinLoader.isLongFluidGasRequirementsEnabled(Arrays.asList(
            "experimental {",
            "    enableLongFluidGasRequirements = TRUE # manual shorthand is accepted",
            "}"
        )));
    }
}

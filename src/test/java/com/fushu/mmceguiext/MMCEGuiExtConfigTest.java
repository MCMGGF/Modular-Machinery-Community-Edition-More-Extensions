package com.fushu.mmceguiext;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MMCEGuiExtConfigTest {
    @Test
    public void legacySmartInterfaceFallbackKeysAreDetected() {
        assertTrue(MMCEGuiExtConfig.isLegacySmartInterfaceEditorFallback("mmcege_virtual_port"));
        assertTrue(MMCEGuiExtConfig.isLegacySmartInterfaceEditorFallback(" demo_default_port_a, demo_default_port_b "));
        assertTrue(MMCEGuiExtConfig.isLegacySmartInterfaceEditorFallback("demo_default_port_b;demo_default_port_a"));
    }

    @Test
    public void customSmartInterfaceFallbackKeysAreKept() {
        assertFalse(MMCEGuiExtConfig.isLegacySmartInterfaceEditorFallback("food"));
        assertFalse(MMCEGuiExtConfig.isLegacySmartInterfaceEditorFallback("demo_default_port_a,food"));
        assertEquals("food", MMCEGuiExtConfig.sanitizeSmartInterfaceEditorVirtualKey("food"));
    }

    @Test
    public void sanitizeClearsOnlyLegacySmartInterfaceFallbackKeys() {
        assertEquals("", MMCEGuiExtConfig.sanitizeSmartInterfaceEditorVirtualKey("mmcege_virtual_port"));
        assertEquals("", MMCEGuiExtConfig.sanitizeSmartInterfaceEditorVirtualKey("demo_default_port_a,demo_default_port_b"));
        assertEquals("mmcege_virtual_port,food", MMCEGuiExtConfig.sanitizeSmartInterfaceEditorVirtualKey("mmcege_virtual_port,food"));
    }

    @Test
    public void jsonCustomContentIsOptInByDefault() {
        MMCEGuiExtConfig.CustomContent customContent = new MMCEGuiExtConfig.CustomContent();

        assertFalse(customContent.enableCustomHatches);
        assertFalse(customContent.enableCustomAEBuses);
        assertTrue(customContent.registerGenericCustomHatch);
    }

    @Test
    public void experimentalLongFluidGasRequirementsAreDisabledByDefault() {
        MMCEGuiExtConfig.Experimental experimental = new MMCEGuiExtConfig.Experimental();

        assertFalse(experimental.enableLongFluidGasRequirements);
    }

    @Test
    public void customParallelControllerTiersAreOptInWithConservativeDefaults() {
        MMCEGuiExtConfig.Experimental experimental = new MMCEGuiExtConfig.Experimental();

        assertFalse(experimental.enableCustomParallelControllerTiers);
        assertEquals(1, experimental.customParallelControllerTierCount);
        assertEquals(32, experimental.customParallelControllerDefaultMaxParallelism);
        assertEquals(0, experimental.customParallelControllerMaxParallelisms.length);
    }

    @Test
    public void guiConfigFileSizeLimitUsesMiBAndClampsInvalidValues() {
        int previous = MMCEGuiExtConfig.maxGuiConfigFileSizeMiB;
        assertEquals(8, previous);
        try {
            MMCEGuiExtConfig.maxGuiConfigFileSizeMiB = 8;
            assertEquals(8L * 1024L * 1024L, MMCEGuiExtConfig.getMaxExtensionConfigFileBytes());
            assertFalse(MMCEGuiExtConfig.isExtensionConfigFileSizeAllowed(-1L));
            assertTrue(MMCEGuiExtConfig.isExtensionConfigFileSizeAllowed(8L * 1024L * 1024L));
            assertFalse(MMCEGuiExtConfig.isExtensionConfigFileSizeAllowed(8L * 1024L * 1024L + 1L));

            MMCEGuiExtConfig.maxGuiConfigFileSizeMiB = 1;
            assertEquals(1L * 1024L * 1024L, MMCEGuiExtConfig.getMaxExtensionConfigFileBytes());
            MMCEGuiExtConfig.maxGuiConfigFileSizeMiB = 64;
            assertEquals(64L * 1024L * 1024L, MMCEGuiExtConfig.getMaxExtensionConfigFileBytes());

            MMCEGuiExtConfig.maxGuiConfigFileSizeMiB = 0;
            assertEquals(1L * 1024L * 1024L, MMCEGuiExtConfig.getMaxExtensionConfigFileBytes());
            MMCEGuiExtConfig.maxGuiConfigFileSizeMiB = 65;
            assertEquals(64L * 1024L * 1024L, MMCEGuiExtConfig.getMaxExtensionConfigFileBytes());
        } finally {
            MMCEGuiExtConfig.maxGuiConfigFileSizeMiB = previous;
        }
    }

    @Test
    public void legacyGuiLimitMethodsDelegateToSharedExtensionLimit() {
        int previous = MMCEGuiExtConfig.maxGuiConfigFileSizeMiB;
        try {
            MMCEGuiExtConfig.maxGuiConfigFileSizeMiB = 16;
            assertEquals(
                MMCEGuiExtConfig.getMaxExtensionConfigFileBytes(),
                MMCEGuiExtConfig.getMaxGuiConfigFileBytes()
            );
            assertEquals(
                MMCEGuiExtConfig.isExtensionConfigFileSizeAllowed(16L * 1024L * 1024L),
                MMCEGuiExtConfig.isGuiConfigFileSizeAllowed(16L * 1024L * 1024L)
            );
        } finally {
            MMCEGuiExtConfig.maxGuiConfigFileSizeMiB = previous;
        }
    }
}

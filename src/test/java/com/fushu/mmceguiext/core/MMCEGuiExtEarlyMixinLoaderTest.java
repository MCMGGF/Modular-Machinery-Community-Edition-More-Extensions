package com.fushu.mmceguiext.core;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MMCEGuiExtEarlyMixinLoaderTest {
    @Test
    public void earlyLoaderRegistersOnlyTheBaseMixinConfig() {
        assertEquals(
            Collections.singletonList("mixins.mmceguiext.json"),
            MMCEGuiExtEarlyMixinLoader.ALWAYS_REGISTERED_MIXIN_CONFIGS
        );
    }

    @Test
    public void customParallelControllerSettingsAreParsedAndClamped() {
        assertTrue(MMCEGuiExtEarlyMixinLoader.isCustomParallelControllerTiersEnabled(Arrays.asList(
            "B:enableCustomParallelControllerTiers=true",
            "I:customParallelControllerTierCount=99",
            "I:customParallelControllerDefaultMaxParallelism=48",
            "S:customParallelControllerMaxParallelisms <",
            "    16",
            "    64",
            ">"
        )));
        assertEquals(16, MMCEGuiExtEarlyMixinLoader.getCustomParallelControllerTierCount());
        assertEquals(16, MMCEGuiExtEarlyMixinLoader.getCustomParallelControllerMaxParallelism(0));
        assertEquals(64, MMCEGuiExtEarlyMixinLoader.getCustomParallelControllerMaxParallelism(1));
        assertEquals(48, MMCEGuiExtEarlyMixinLoader.getCustomParallelControllerMaxParallelism(2));

        assertFalse(MMCEGuiExtEarlyMixinLoader.isCustomParallelControllerTiersEnabled(
            Collections.singletonList("B:enableCustomParallelControllerTiers=false")
        ));
    }
}

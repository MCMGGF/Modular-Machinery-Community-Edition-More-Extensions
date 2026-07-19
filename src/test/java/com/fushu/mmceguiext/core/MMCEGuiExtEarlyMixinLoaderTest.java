package com.fushu.mmceguiext.core;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class MMCEGuiExtEarlyMixinLoaderTest {
    @Test
    public void earlyLoaderRegistersOnlyTheBaseMixinConfig() {
        assertEquals(
            Collections.singletonList("mixins.mmceguiext.json"),
            MMCEGuiExtEarlyMixinLoader.ALWAYS_REGISTERED_MIXIN_CONFIGS
        );
    }
}

package com.fushu.mmceguiext.mixin;

import com.fushu.mmceguiext.core.MMCEGuiExtEarlyMixinLoader;
import hellfirepvp.modularmachinery.common.block.prop.ParallelControllerData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Extends MMCE's native enum so the existing block, tile, GUI and recipe
 * parallelism paths also understand the additional controller tiers.
 */
@Mixin(value = ParallelControllerData.class, remap = false, priority = 900)
public abstract class MixinParallelControllerData {
    @Unique
    private static final Logger LOGGER = LogManager.getLogger("MMCEME");

    @Shadow
    @Final
    @Mutable
    private static ParallelControllerData[] $VALUES;

    @Invoker(value = "<init>", remap = false)
    private static ParallelControllerData mmceme$invokeNew(
        String name,
        int ordinal,
        int defaultMaxParallelism
    ) {
        return null;
    }

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void mmceme$appendParallelControllerTiers(CallbackInfo ci) {
        if (!MMCEGuiExtEarlyMixinLoader.areCustomParallelControllerTiersEnabled()) {
            return;
        }

        if (hasTierPrefix("whimcraft_")) {
            LOGGER.warn(
                "WhimCraft extra parallel controller tiers were detected; " +
                    "MMCEME custom parallel controller tiers will not be added."
            );
            return;
        }
        if (hasTierPrefix("mmceme_")) {
            return;
        }

        int count = MMCEGuiExtEarlyMixinLoader.getCustomParallelControllerTierCount();
        List<ParallelControllerData> values = new ArrayList<ParallelControllerData>(Arrays.asList($VALUES));
        int nextOrdinal = values.size();
        for (int index = 0; index < count; index++) {
            String name = "MMCEME_" + index;
            int maxParallelism = MMCEGuiExtEarlyMixinLoader.getCustomParallelControllerMaxParallelism(index);
            values.add(mmceme$invokeNew(name, nextOrdinal++, maxParallelism));
        }
        $VALUES = values.toArray(new ParallelControllerData[values.size()]);
        LOGGER.info("Registered {} MMCEME custom parallel controller tier(s).", count);
    }

    @Unique
    private static boolean hasTierPrefix(String prefix) {
        for (ParallelControllerData data : $VALUES) {
            if (data != null && data.getName().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}

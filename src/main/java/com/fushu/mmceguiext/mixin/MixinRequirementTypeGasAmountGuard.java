package com.fushu.mmceguiext.mixin;

import com.fushu.mmceguiext.common.requirement.LongRequirementJson;
import com.google.gson.JsonObject;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGas;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementTypeGas;
import hellfirepvp.modularmachinery.common.machine.IOType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RequirementTypeGas.class, remap = false)
public abstract class MixinRequirementTypeGasAmountGuard {
    @Inject(method = "createRequirement", at = @At("HEAD"))
    private void mmceguiext$rejectOversizedVanillaAmount(IOType type,
                                                          JsonObject requirement,
                                                          CallbackInfoReturnable<RequirementGas> cir) {
        LongRequirementJson.rejectOversizedVanillaAmount(
            requirement,
            "gas",
            "mmceguiext:gas_long"
        );
    }
}

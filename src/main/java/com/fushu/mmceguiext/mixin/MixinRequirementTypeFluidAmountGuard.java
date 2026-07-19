package com.fushu.mmceguiext.mixin;

import com.fushu.mmceguiext.common.requirement.LongRequirementJson;
import com.google.gson.JsonObject;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementTypeFluid;
import hellfirepvp.modularmachinery.common.machine.IOType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RequirementTypeFluid.class, remap = false)
public abstract class MixinRequirementTypeFluidAmountGuard {
    @Inject(method = "createRequirement", at = @At("HEAD"))
    private void mmceguiext$rejectOversizedVanillaAmount(IOType type,
                                                          JsonObject requirement,
                                                          CallbackInfoReturnable<RequirementFluid> cir) {
        LongRequirementJson.rejectOversizedVanillaAmount(
            requirement,
            "fluid",
            "mmceguiext:fluid_long"
        );
    }
}

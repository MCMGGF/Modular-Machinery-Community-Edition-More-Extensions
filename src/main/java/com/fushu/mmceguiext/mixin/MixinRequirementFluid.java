package com.fushu.mmceguiext.mixin;

import com.fushu.mmceguiext.MMCEGuiExtConfig;
import com.fushu.mmceguiext.common.requirement.LongRequirementIO;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementTypeFluid;
import hellfirepvp.modularmachinery.common.machine.IOType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = RequirementFluid.class, remap = false)
public abstract class MixinRequirementFluid extends ComponentRequirement.MultiCompParallelizable<Object, RequirementTypeFluid> {
    private MixinRequirementFluid(RequirementTypeFluid requirementType, IOType actionType) {
        super(requirementType, actionType);
    }

    @Inject(method = "copyComponents", at = @At("HEAD"), cancellable = true)
    private void mmceguiext$copySharedLongSnapshots(List<ProcessingComponent<?>> components,
                                                     CallbackInfoReturnable<List<ProcessingComponent<?>>> cir) {
        if (MMCEGuiExtConfig.isLongFluidGasRequirementsEnabled()) {
            cir.setReturnValue(LongRequirementIO.copyFluidComponents(components));
        }
    }
}

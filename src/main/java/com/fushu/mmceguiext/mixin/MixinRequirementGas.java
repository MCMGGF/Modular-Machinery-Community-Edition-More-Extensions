package com.fushu.mmceguiext.mixin;

import com.fushu.mmceguiext.MMCEGuiExtConfig;
import com.fushu.mmceguiext.common.requirement.LongGasRequirementIO;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGas;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementTypeGas;
import hellfirepvp.modularmachinery.common.machine.IOType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = RequirementGas.class, remap = false)
public abstract class MixinRequirementGas extends ComponentRequirement.MultiCompParallelizable<Object, RequirementTypeGas> {
    private MixinRequirementGas(RequirementTypeGas requirementType, IOType actionType) {
        super(requirementType, actionType);
    }

    @Inject(method = "copyComponents", at = @At("HEAD"), cancellable = true)
    private void mmceguiext$copySharedLongSnapshots(List<ProcessingComponent<?>> components,
                                                     CallbackInfoReturnable<List<ProcessingComponent<?>>> cir) {
        if (MMCEGuiExtConfig.isLongFluidGasRequirementsEnabled()) {
            cir.setReturnValue(LongGasRequirementIO.copyGasComponents(components));
        }
    }
}

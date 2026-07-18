package com.fushu.mmceguiext.mixin;

import hellfirepvp.modularmachinery.common.crafting.helper.ComponentOutputRestrictor;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementInterfaceNumInput;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = RequirementInterfaceNumInput.class)
public abstract class MixinRequirementInterfaceNumInput {

    @Final
    @Shadow(remap = false)
    protected SmartInterfaceType type;

    @Final
    @Shadow(remap = false)
    protected float minValue;

    @Final
    @Shadow(remap = false)
    protected float maxValue;

    @Inject(method = "isValidComponent", at = @At("HEAD"), cancellable = true, remap = false)
    private void mmceguiext$acceptVirtualSmartInterface(
        final ProcessingComponent<?> component,
        final RecipeCraftingContext context,
        final CallbackInfoReturnable<Boolean> cir
    ) {
        MachineComponent<?> machineComponent =
            component == null ? null : component.component();
        boolean valid = machineComponent != null
            && machineComponent.getComponentType().equals(ComponentTypesMM.COMPONENT_SMART_INTERFACE)
            && context != null
            && context.getMachineController() != null
            && context.getMachineController().getSmartInterfaceData(type.getType()) != null;
        cir.setReturnValue(valid);
    }

    @Inject(method = "canStartCrafting", at = @At("HEAD"), cancellable = true, remap = false)
    private void mmceguiext$readSmartInterfaceFromController(
        final ProcessingComponent<?> component,
        final RecipeCraftingContext context,
        final List<ComponentOutputRestrictor> restrictions,
        final CallbackInfoReturnable<CraftCheck> cir
    ) {
        SmartInterfaceData data = context == null || context.getMachineController() == null
            ? null
            : context.getMachineController().getSmartInterfaceData(type.getType());
        if (data == null) {
            cir.setReturnValue(
                CraftCheck.failure("component.missing.modularmachinery.interface.number")
            );
            return;
        }

        float value = data.getValue();
        if (value >= minValue && value <= maxValue) {
            cir.setReturnValue(CraftCheck.success());
            return;
        }

        String customMessage = type.getNotEqualMessage();
        cir.setReturnValue(
            CraftCheck.failure(
                customMessage.isEmpty()
                    ? "craftcheck.failure.interface.number.notequal"
                    : customMessage
            )
        );
    }
}

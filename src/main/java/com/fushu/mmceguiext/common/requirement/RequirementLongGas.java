package com.fushu.mmceguiext.common.requirement;

import github.kasuminova.mmce.common.util.IExtendedGasHandler;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGas;
import hellfirepvp.modularmachinery.common.crafting.requirement.jei.JEIComponentItem;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.HybridFluidUtils;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nonnull;
import java.awt.Point;
import java.util.Collections;
import java.util.List;

@Optional.Interface(modid = "mekanism", iface = "github.kasuminova.mmce.common.util.IExtendedGasHandler")
public class RequirementLongGas extends RequirementGas {
    private final long requiredAmount;

    public RequirementLongGas(IOType actionType, Gas gas, long requiredAmount) {
        super(actionType, new GasStack(gas, LongRequirementAmounts.downcastAmount(requiredAmount)));
        this.requiredAmount = LongRequirementAmounts.sanitizeAmount(requiredAmount);
    }

    public long getRequiredAmountLong() {
        return this.requiredAmount;
    }

    @Override
    @Optional.Method(modid = "mekanism")
    public RequirementLongGas deepCopy() {
        return deepCopyModified(Collections.<RecipeModifier>emptyList());
    }

    @Override
    @Optional.Method(modid = "mekanism")
    public RequirementLongGas deepCopyModified(List<RecipeModifier> modifiers) {
        RequirementLongGas copy = new RequirementLongGas(
            this.actionType,
            this.required.getGas(),
            LongRequirementAmounts.applyModifiers(modifiers, this, this.requiredAmount)
        );
        copy.chance = RecipeModifier.applyModifiers(modifiers, this, this.chance, true);
        return copy;
    }

    @Override
    @Optional.Method(modid = "mekanism")
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ComponentRequirement.JEIComponent provideJEIComponent() {
        return new LongGasJeiComponent(this);
    }

    @Override
    @Optional.Method(modid = "mekanism")
    public void startCrafting(List<ProcessingComponent<?>> components,
                              RecipeCraftingContext context,
                              ResultChance chance) {
        if (this.actionType == IOType.INPUT
            && chance.canWork(RecipeModifier.applyModifiers(context, this, this.chance, true))) {
            doGasIO(components, context);
        }
    }

    @Override
    @Optional.Method(modid = "mekanism")
    public void finishCrafting(List<ProcessingComponent<?>> components,
                               RecipeCraftingContext context,
                               ResultChance chance) {
        if (this.actionType == IOType.OUTPUT
            && chance.canWork(RecipeModifier.applyModifiers(context, this, this.chance, true))) {
            doGasIO(components, context);
        }
    }

    @Nonnull
    @Override
    @Optional.Method(modid = "mekanism")
    public CraftCheck canStartCrafting(List<ProcessingComponent<?>> components,
                                       RecipeCraftingContext context) {
        return doGasIO(components, context);
    }

    @Nonnull
    @Override
    @Optional.Method(modid = "mekanism")
    public List<ProcessingComponent<?>> copyComponents(List<ProcessingComponent<?>> components) {
        return LongGasRequirementIO.copyGasComponents(components);
    }

    @Override
    @Optional.Method(modid = "mekanism")
    public int getMaxParallelism(List<ProcessingComponent<?>> components,
                                 RecipeCraftingContext context,
                                 int maxParallelism) {
        if (this.ignoreOutputCheck && this.actionType == IOType.OUTPUT) {
            return maxParallelism;
        }
        if (this.parallelizeUnaffected) {
            return doGasIOInternal(components, context, 1) >= 1 ? maxParallelism : 0;
        }
        return doGasIOInternal(components, context, maxParallelism);
    }

    @Optional.Method(modid = "mekanism")
    private CraftCheck doGasIO(List<ProcessingComponent<?>> components,
                               RecipeCraftingContext context) {
        int completed = doGasIOInternal(components, context, this.parallelism);
        if (completed >= this.parallelism) {
            return CraftCheck.success();
        }
        if (this.actionType == IOType.INPUT) {
            return CraftCheck.failure("craftcheck.failure.gas.input");
        }
        return this.ignoreOutputCheck
            ? CraftCheck.success()
            : CraftCheck.failure("craftcheck.failure.gas.output.space");
    }

    @Optional.Method(modid = "mekanism")
    private int doGasIOInternal(List<ProcessingComponent<?>> components,
                                RecipeCraftingContext context,
                                int maxMultiplier) {
        if (maxMultiplier <= 0) {
            return 0;
        }
        long required = LongRequirementAmounts.applyModifiers(context, this, this.requiredAmount);
        if (required <= 0L) {
            return maxMultiplier;
        }
        long maxRequired = LongRequirementAmounts.saturatedMultiply(required, maxMultiplier);
        List<IExtendedGasHandler> handlers = HybridFluidUtils.castGasHandlerComponents(components);
        long available = LongGasRequirementIO.simulateGas(this.required, handlers, maxRequired, this.actionType);
        long completedAmount = LongRequirementAmounts.completeAmount(available, required, maxMultiplier);
        if (completedAmount <= 0L) {
            return 0;
        }
        LongGasRequirementIO.doGas(this.required, handlers, completedAmount, this.actionType);
        return (int) (completedAmount / required);
    }

    private static final class LongGasJeiComponent extends ComponentRequirement.JEIComponent<GasStack> {
        private final RequirementLongGas requirement;

        private LongGasJeiComponent(RequirementLongGas requirement) {
            this.requirement = requirement;
        }

        @Override
        public Class<GasStack> getJEIRequirementClass() {
            return GasStack.class;
        }

        @Override
        public List<GasStack> getJEIIORequirements() {
            GasStack display = this.requirement.required.copy();
            display.amount = LongRequirementAmounts.downcastAmount(this.requirement.requiredAmount);
            return Collections.singletonList(display);
        }

        @Override
        public RecipeLayoutPart<GasStack> getLayoutPart(Point offset) {
            return new RecipeLayoutPart.GasTank(offset);
        }

        @Override
        public void onJEIHoverTooltip(int slotIndex,
                                      boolean input,
                                      GasStack ingredient,
                                      List<String> tooltip) {
            tooltip.add("Amount: " + this.requirement.requiredAmount + " mB");
            JEIComponentItem.addChanceTooltip(input, tooltip, this.requirement.chance);
        }
    }
}

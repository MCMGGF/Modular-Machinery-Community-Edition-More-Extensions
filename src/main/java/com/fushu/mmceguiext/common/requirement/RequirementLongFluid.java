package com.fushu.mmceguiext.common.requirement;

import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid;
import hellfirepvp.modularmachinery.common.crafting.requirement.jei.JEIComponentItem;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.HybridFluidUtils;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.awt.Point;
import java.util.Collections;
import java.util.List;

public class RequirementLongFluid extends RequirementFluid {
    private final long requiredAmount;

    public RequirementLongFluid(IOType actionType, Fluid fluid, long requiredAmount) {
        super(actionType, new FluidStack(fluid, LongRequirementAmounts.downcastAmount(requiredAmount)));
        this.requiredAmount = LongRequirementAmounts.sanitizeAmount(requiredAmount);
    }

    public long getRequiredAmountLong() {
        return this.requiredAmount;
    }

    @Override
    public RequirementLongFluid deepCopy() {
        return deepCopyModified(Collections.<RecipeModifier>emptyList());
    }

    @Override
    public RequirementLongFluid deepCopyModified(List<RecipeModifier> modifiers) {
        RequirementLongFluid copy = new RequirementLongFluid(
            this.actionType,
            this.required.getFluid(),
            LongRequirementAmounts.applyModifiers(modifiers, this, this.requiredAmount)
        );
        copy.chance = RecipeModifier.applyModifiers(modifiers, this, this.chance, true);
        copy.setMatchNBTTag(getTagMatch());
        copy.setDisplayNBTTag(getTagDisplay());
        return copy;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ComponentRequirement.JEIComponent provideJEIComponent() {
        return new LongFluidJeiComponent(this);
    }

    @Override
    public void startCrafting(List<ProcessingComponent<?>> components,
                              RecipeCraftingContext context,
                              ResultChance chance) {
        if (this.actionType == IOType.INPUT
            && chance.canWork(RecipeModifier.applyModifiers(context, this, this.chance, true))) {
            doFluidIO(components, context);
        }
    }

    @Override
    public void finishCrafting(List<ProcessingComponent<?>> components,
                               RecipeCraftingContext context,
                               ResultChance chance) {
        if (this.actionType == IOType.OUTPUT
            && chance.canWork(RecipeModifier.applyModifiers(context, this, this.chance, true))) {
            doFluidIO(components, context);
        }
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(List<ProcessingComponent<?>> components,
                                       RecipeCraftingContext context) {
        return doFluidIO(components, context);
    }

    @Nonnull
    @Override
    public List<ProcessingComponent<?>> copyComponents(List<ProcessingComponent<?>> components) {
        return LongRequirementIO.copyFluidComponents(components);
    }

    @Override
    public int getMaxParallelism(List<ProcessingComponent<?>> components,
                                 RecipeCraftingContext context,
                                 int maxParallelism) {
        if (this.ignoreOutputCheck && this.actionType == IOType.OUTPUT) {
            return maxParallelism;
        }
        if (this.parallelizeUnaffected) {
            return doFluidIOInternal(components, context, 1) >= 1 ? maxParallelism : 0;
        }
        return doFluidIOInternal(components, context, maxParallelism);
    }

    private CraftCheck doFluidIO(List<ProcessingComponent<?>> components,
                                 RecipeCraftingContext context) {
        int completed = doFluidIOInternal(components, context, this.parallelism);
        if (completed >= this.parallelism) {
            return CraftCheck.success();
        }
        if (this.actionType == IOType.INPUT) {
            return CraftCheck.failure("craftcheck.failure.fluid.input");
        }
        return this.ignoreOutputCheck
            ? CraftCheck.success()
            : CraftCheck.failure("craftcheck.failure.fluid.output.space");
    }

    private int doFluidIOInternal(List<ProcessingComponent<?>> components,
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
        List<IFluidHandler> handlers = HybridFluidUtils.castFluidHandlerComponents(components);
        long available = LongRequirementIO.simulateFluid(this.required, handlers, maxRequired, this.actionType);
        if (available < required) {
            return 0;
        }
        LongRequirementIO.doFluid(this.required, handlers, available, this.actionType);
        return (int) Math.min((long) maxMultiplier, available / required);
    }

    private static final class LongFluidJeiComponent extends ComponentRequirement.JEIComponent<FluidStack> {
        private final RequirementLongFluid requirement;

        private LongFluidJeiComponent(RequirementLongFluid requirement) {
            this.requirement = requirement;
        }

        @Override
        public Class<FluidStack> getJEIRequirementClass() {
            return FluidStack.class;
        }

        @Override
        public List<FluidStack> getJEIIORequirements() {
            FluidStack display = this.requirement.required.copy();
            display.amount = LongRequirementAmounts.downcastAmount(this.requirement.requiredAmount);
            return Collections.singletonList(display);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public RecipeLayoutPart<FluidStack> getLayoutPart(Point offset) {
            return new RecipeLayoutPart.FluidTank(offset);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void onJEIHoverTooltip(int slotIndex,
                                      boolean input,
                                      FluidStack ingredient,
                                      List<String> tooltip) {
            tooltip.add("Amount: " + this.requirement.requiredAmount + " mB");
            JEIComponentItem.addChanceTooltip(input, tooltip, this.requirement.chance);
        }
    }
}

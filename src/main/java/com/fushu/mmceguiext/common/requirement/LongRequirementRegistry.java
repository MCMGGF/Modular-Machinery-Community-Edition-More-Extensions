package com.fushu.mmceguiext.common.requirement;

import com.fushu.mmceguiext.MMCEGuiExt;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = MMCEGuiExt.MODID)
public final class LongRequirementRegistry {
    public static final ResourceLocation FLUID_LONG =
        new ResourceLocation(MMCEGuiExt.MODID, "fluid_long");
    public static final ResourceLocation GAS_LONG =
        new ResourceLocation(MMCEGuiExt.MODID, "gas_long");

    private LongRequirementRegistry() {
    }

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerRequirementTypes(RegistryEvent.Register event) {
        if (event.getGenericType() != RequirementType.class) {
            return;
        }
        RequirementTypeLongFluid fluid = new RequirementTypeLongFluid();
        fluid.setRegistryName(FLUID_LONG);
        event.getRegistry().register(fluid);

        RequirementTypeLongGas gas = new RequirementTypeLongGas();
        gas.setRegistryName(GAS_LONG);
        event.getRegistry().register(gas);
    }
}

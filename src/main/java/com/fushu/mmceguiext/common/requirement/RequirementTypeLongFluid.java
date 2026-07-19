package com.fushu.mmceguiext.common.requirement;

import com.fushu.mmceguiext.MMCEGuiExtConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementTypeFluid;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.util.nbt.NBTJsonDeserializer;
import net.minecraft.nbt.NBTException;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

public class RequirementTypeLongFluid extends RequirementTypeFluid {
    @Override
    public RequirementLongFluid createRequirement(IOType type, JsonObject requirement) {
        if (!MMCEGuiExtConfig.isLongFluidGasRequirementsEnabled()) {
            throw new JsonParseException(
                "Requirement type 'mmceguiext:fluid_long' is experimental and disabled. "
                    + "Set experimental.enableLongFluidGasRequirements=true in "
                    + "config/mmceguiext/client.cfg, then restart the game/server."
            );
        }
        if (!requirement.has("fluid") || !requirement.get("fluid").isJsonPrimitive()
            || !requirement.getAsJsonPrimitive("fluid").isString()) {
            throw new JsonParseException("The requirement type 'mmceguiext:fluid_long' expects a string 'fluid' entry.");
        }
        String fluidName = requirement.getAsJsonPrimitive("fluid").getAsString();
        Fluid fluid = FluidRegistry.getFluid(fluidName);
        if (fluid == null) {
            throw new JsonParseException("The fluid specified by 'fluid' does not exist: " + fluidName);
        }

        RequirementLongFluid result = new RequirementLongFluid(type, fluid, LongRequirementJson.parseAmount(
            requirement, "mmceguiext:fluid_long"));
        if (requirement.has("chance")) {
            if (!requirement.get("chance").isJsonPrimitive()
                || !requirement.getAsJsonPrimitive("chance").isNumber()) {
                throw new JsonParseException("'chance', if defined, must be a number between 0 and 1.");
            }
            float chance = requirement.getAsJsonPrimitive("chance").getAsFloat();
            if (chance < 0F || chance > 1F) {
                throw new JsonParseException("'chance' must be between 0 and 1.");
            }
            result.setChance(chance);
        }
        if (requirement.has("nbt")) {
            if (!requirement.get("nbt").isJsonObject()) {
                throw new JsonParseException("'nbt', if defined, must be a JSON object.");
            }
            try {
                result.setMatchNBTTag(NBTJsonDeserializer.deserialize(requirement.getAsJsonObject("nbt").toString()));
                if (requirement.has("nbt-display")) {
                    if (!requirement.get("nbt-display").isJsonObject()) {
                        throw new JsonParseException("'nbt-display', if defined, must be a JSON object.");
                    }
                    result.setDisplayNBTTag(NBTJsonDeserializer.deserialize(
                        requirement.getAsJsonObject("nbt-display").toString()));
                } else {
                    result.setDisplayNBTTag(result.getTagMatch());
                }
            } catch (NBTException e) {
                throw new JsonParseException("Failed to parse fluid requirement NBT.", e);
            }
        }
        return result;
    }
}

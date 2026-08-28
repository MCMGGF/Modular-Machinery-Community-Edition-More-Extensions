package com.fushu.mmceguiext.common.requirement;

import com.fushu.mmceguiext.MMCEGuiExtConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementTypeGas;
import hellfirepvp.modularmachinery.common.machine.IOType;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nullable;

public class RequirementTypeLongGas extends RequirementTypeGas {
    @Nullable
    @Override
    public String requiresModid() {
        return "mekanism";
    }

    @Override
    @Optional.Method(modid = "mekanism")
    public RequirementLongGas createRequirement(IOType type, JsonObject requirement) {
        if (!MMCEGuiExtConfig.isLongFluidGasRequirementsEnabled()) {
            throw new JsonParseException(
                "Requirement type 'mmceguiext:gas_long' is experimental and disabled. "
                    + "Set experimental.enableLongFluidGasRequirements=true in "
                    + "config/mmceguiext/client.cfg, then restart the game/server."
            );
        }
        if (!requirement.has("gas") || !requirement.get("gas").isJsonPrimitive()
            || !requirement.getAsJsonPrimitive("gas").isString()) {
            throw new JsonParseException("The requirement type 'mmceguiext:gas_long' expects a string 'gas' entry.");
        }
        String gasName = requirement.getAsJsonPrimitive("gas").getAsString();
        Gas gas = GasRegistry.getGas(gasName);
        if (gas == null) {
            throw new JsonParseException("The gas specified by 'gas' does not exist: " + gasName);
        }

        RequirementLongGas result = new RequirementLongGas(type, gas, LongRequirementJson.parseAmount(
            requirement, "mmceguiext:gas_long"));
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
        return result;
    }
}

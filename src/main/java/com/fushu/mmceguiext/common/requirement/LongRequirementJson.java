package com.fushu.mmceguiext.common.requirement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.math.BigDecimal;

public final class LongRequirementJson {
    private LongRequirementJson() {
    }

    public static long parseAmount(JsonObject requirement, String requirementType) {
        if (requirement == null || !requirement.has("amount")) {
            throw new JsonParseException("The requirement type '" + requirementType
                + "' expects an 'amount' entry containing a non-negative integer.");
        }
        JsonElement amountElement = requirement.get("amount");
        if (amountElement == null || !amountElement.isJsonPrimitive()) {
            throw invalidAmount(requirementType, null);
        }
        String raw = amountElement.getAsJsonPrimitive().getAsString();
        if (raw == null || !raw.matches("[0-9]+")) {
            throw invalidAmount(requirementType, raw);
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new JsonParseException("The 'amount' for requirement type '" + requirementType
                + "' is outside the signed 64-bit integer range: " + raw, e);
        }
    }

    public static void rejectOversizedVanillaAmount(JsonObject requirement,
                                                     String vanillaType,
                                                     String longType) {
        if (requirement == null || !requirement.has("amount")) {
            return;
        }
        JsonElement amountElement = requirement.get("amount");
        if (amountElement == null
            || !amountElement.isJsonPrimitive()
            || !amountElement.getAsJsonPrimitive().isNumber()) {
            return;
        }
        try {
            BigDecimal amount = new BigDecimal(amountElement.getAsString());
            if (amount.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
                throw new JsonParseException(
                    "Requirement type '" + vanillaType + "' only supports amount <= "
                        + Integer.MAX_VALUE + ". Use '" + longType
                        + "' and enable experimental.enableLongFluidGasRequirements instead."
                );
            }
        } catch (NumberFormatException ignored) {
            // The original MMCE parser reports malformed numeric values.
        }
    }

    private static JsonParseException invalidAmount(String requirementType, String raw) {
        return new JsonParseException("The 'amount' for requirement type '" + requirementType
            + "' must be a non-negative integer written as a JSON integer or decimal string; found: " + raw);
    }
}

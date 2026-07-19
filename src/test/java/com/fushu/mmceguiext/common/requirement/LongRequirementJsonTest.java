package com.fushu.mmceguiext.common.requirement;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LongRequirementJsonTest {
    private static final String REQUIREMENT_TYPE = "mmceguiext:fluid_long";

    @Test
    public void parseAmountAcceptsJsonNumber() {
        assertEquals(42L, LongRequirementJson.parseAmount(requirementWithAmount(42), REQUIREMENT_TYPE));
    }

    @Test
    public void parseAmountAcceptsDecimalStringDigits() {
        assertEquals(42L, LongRequirementJson.parseAmount(requirementWithAmount("42"), REQUIREMENT_TYPE));
    }

    @Test
    public void parseAmountRejectsNegativeNumber() {
        assertInvalidAmount(-1, "must be a non-negative integer");
    }

    @Test
    public void parseAmountRejectsDecimalNumber() {
        assertInvalidAmount(1.5, "must be a non-negative integer");
    }

    @Test
    public void parseAmountRejectsOutOfRangeNumber() {
        assertInvalidAmount("9223372036854775808", "outside the signed 64-bit integer range");
    }

    @Test
    public void vanillaAmountGuardRejectsValuesAboveIntegerMax() {
        JsonObject requirement = requirementWithAmount("2147483648");
        requirement.addProperty("amount", 2147483648L);
        try {
            LongRequirementJson.rejectOversizedVanillaAmount(
                requirement,
                "fluid",
                "mmceguiext:fluid_long"
            );
            fail("Expected a JsonParseException for an oversized vanilla fluid requirement.");
        } catch (JsonParseException e) {
            assertTrue(e.getMessage().contains("mmceguiext:fluid_long"));
        }
    }

    @Test
    public void vanillaAmountGuardKeepsIntegerRangeValues() {
        LongRequirementJson.rejectOversizedVanillaAmount(
            requirementWithAmount(Integer.MAX_VALUE),
            "fluid",
            "mmceguiext:fluid_long"
        );
    }

    private static void assertInvalidAmount(Number amount, String expectedMessagePart) {
        try {
            LongRequirementJson.parseAmount(requirementWithAmount(amount), REQUIREMENT_TYPE);
            fail("Expected a JsonParseException for amount: " + amount);
        } catch (JsonParseException e) {
            assertTrue(e.getMessage().contains(REQUIREMENT_TYPE));
            assertTrue(e.getMessage().contains(expectedMessagePart));
        }
    }

    private static void assertInvalidAmount(String amount, String expectedMessagePart) {
        try {
            LongRequirementJson.parseAmount(requirementWithAmount(amount), REQUIREMENT_TYPE);
            fail("Expected a JsonParseException for amount: " + amount);
        } catch (JsonParseException e) {
            assertTrue(e.getMessage().contains(REQUIREMENT_TYPE));
            assertTrue(e.getMessage().contains(expectedMessagePart));
        }
    }

    private static JsonObject requirementWithAmount(Number amount) {
        JsonObject requirement = new JsonObject();
        requirement.addProperty("amount", amount);
        return requirement;
    }

    private static JsonObject requirementWithAmount(String amount) {
        JsonObject requirement = new JsonObject();
        requirement.addProperty("amount", amount);
        return requirement;
    }
}

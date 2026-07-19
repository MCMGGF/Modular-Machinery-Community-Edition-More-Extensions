package com.fushu.mmceguiext.common.requirement;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LongRequirementAmountsTest {
    @Test
    public void maxExactDoubleIntegerMatchesTheDoublePrecisionBoundary() {
        assertEquals(9_007_199_254_740_991L, LongRequirementAmounts.MAX_EXACT_DOUBLE_INTEGER);
    }

    @Test
    public void sanitizeAmountClampsNegativeValuesToZero() {
        assertEquals(0L, LongRequirementAmounts.sanitizeAmount(-1L));
    }

    @Test
    public void downcastAmountClampsLargeValuesToIntegerMaxValue() {
        assertEquals(Integer.MAX_VALUE, LongRequirementAmounts.downcastAmount(Long.MAX_VALUE));
    }

    @Test
    public void saturatedMultiplyPreservesNormalProducts() {
        assertEquals(42L, LongRequirementAmounts.saturatedMultiply(6L, 7));
    }

    @Test
    public void saturatedMultiplyReturnsZeroForNonPositiveInputs() {
        assertEquals(0L, LongRequirementAmounts.saturatedMultiply(0L, 7));
        assertEquals(0L, LongRequirementAmounts.saturatedMultiply(7L, 0));
        assertEquals(0L, LongRequirementAmounts.saturatedMultiply(-1L, 7));
    }

    @Test
    public void saturatedMultiplySaturatesOnOverflow() {
        assertEquals(Long.MAX_VALUE, LongRequirementAmounts.saturatedMultiply(Long.MAX_VALUE / 2 + 1, 3));
    }

    @Test
    public void saturatedAddPreservesNormalSums() {
        assertEquals(42L, LongRequirementAmounts.saturatedAdd(17L, 25L));
    }

    @Test
    public void saturatedAddReturnsTheNonNegativeOperandWhenTheOtherSideIsNonPositive() {
        assertEquals(17L, LongRequirementAmounts.saturatedAdd(17L, 0L));
        assertEquals(25L, LongRequirementAmounts.saturatedAdd(0L, 25L));
        assertEquals(0L, LongRequirementAmounts.saturatedAdd(-5L, -1L));
    }

    @Test
    public void saturatedAddSaturatesOnOverflow() {
        assertEquals(Long.MAX_VALUE, LongRequirementAmounts.saturatedAdd(Long.MAX_VALUE - 1, 10L));
    }
}

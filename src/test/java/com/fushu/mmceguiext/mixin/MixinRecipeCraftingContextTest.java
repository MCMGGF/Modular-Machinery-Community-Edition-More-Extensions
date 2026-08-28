package com.fushu.mmceguiext.mixin;

import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class MixinRecipeCraftingContextTest {
    @Test
    public void nullOnlyCandidateListsAreTreatedAsEmpty() {
        Map<Long, java.util.List<ProcessingComponent<?>>> result =
            new LinkedHashMap<Long, java.util.List<ProcessingComponent<?>>>();
        result.put(Long.valueOf(1L), Arrays.<ProcessingComponent<?>>asList((ProcessingComponent<?>) null));

        assertTrue(MixinRecipeCraftingContext.mmceguiext$isEmptyResult(result));
    }

    @Test
    public void emptyAndNullOnlyResultsAreBothEmpty() {
        Map<Long, java.util.List<ProcessingComponent<?>>> result =
            new LinkedHashMap<Long, java.util.List<ProcessingComponent<?>>>();
        result.put(Long.valueOf(1L), Collections.<ProcessingComponent<?>>singletonList(null));
        result.put(Long.valueOf(2L), Collections.<ProcessingComponent<?>>emptyList());

        assertTrue(MixinRecipeCraftingContext.mmceguiext$isEmptyResult(result));
        assertTrue(MixinRecipeCraftingContext.mmceguiext$isEmptyResult(
            Collections.<Long, java.util.List<ProcessingComponent<?>>>emptyMap()
        ));
    }
}

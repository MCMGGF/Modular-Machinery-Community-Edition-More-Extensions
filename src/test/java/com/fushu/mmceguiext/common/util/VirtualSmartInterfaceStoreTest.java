package com.fushu.mmceguiext.common.util;

import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VirtualSmartInterfaceStoreTest {
    @Test
    public void syncCreatesDefaultEntriesForDeclaredTypes() {
        VirtualSmartInterfaceStore store = new VirtualSmartInterfaceStore();
        DynamicMachine machine = machine(
            "default_machine",
            typed("alpha", 2.5F, 1),
            typed("beta", 6.5F, 3)
        );
        BlockPos pos = new BlockPos(4, 80, -12);

        store.sync(machine, pos);
        SmartInterfaceData[] list = store.list(machine, pos);

        assertEquals(2, list.length);
        assertEquals("beta", list[0].getType());
        assertEquals(6.5F, list[0].getValue(), 0.0001F);
        assertEquals(pos, list[0].getPos());
        assertEquals(machine.getRegistryName(), list[0].getParent());
        assertEquals("alpha", list[1].getType());
        assertEquals(2.5F, list[1].getValue(), 0.0001F);
    }

    @Test
    public void setUpdatesExistingValueAndNormalizesLocation() {
        VirtualSmartInterfaceStore store = new VirtualSmartInterfaceStore();
        DynamicMachine machine = machine("setter_machine", typed("count", 1.0F, 2));
        BlockPos pos = new BlockPos(10, 64, 20);

        store.sync(machine, pos);
        SmartInterfaceData updated = store.set(machine, new BlockPos(11, 65, 21), "count", 9.75F);

        assertNotNull(updated);
        assertEquals("count", updated.getType());
        assertEquals(9.75F, updated.getValue(), 0.0001F);
        assertEquals(new BlockPos(11, 65, 21), updated.getPos());
        assertEquals(machine.getRegistryName(), updated.getParent());
        assertEquals(9.75F, store.get(machine, new BlockPos(11, 65, 21), "count").getValue(), 0.0001F);
    }

    @Test
    public void setRejectsUndeclaredEmptyAndNonFiniteTypes() {
        VirtualSmartInterfaceStore store = new VirtualSmartInterfaceStore();
        DynamicMachine machine = machine("reject_machine", typed("present", 1.0F, 1));
        BlockPos pos = new BlockPos(0, 70, 0);

        store.sync(machine, pos);

        assertNull(store.set(machine, pos, "", 1.0F));
        assertNull(store.set(machine, pos, "missing", 1.0F));
        assertNull(store.set(machine, pos, "present", Float.NaN));
        assertNull(store.set(machine, pos, "present", Float.POSITIVE_INFINITY));
        assertEquals(1, store.list(machine, pos).length);
    }

    @Test
    public void syncRemovesExpiredTypesWhilePreservingExistingValues() {
        VirtualSmartInterfaceStore store = new VirtualSmartInterfaceStore();
        DynamicMachine initial = machine(
            "initial_machine",
            typed("keep", 2.0F, 1),
            typed("drop", 3.0F, 5)
        );
        DynamicMachine replacement = machine("replacement_machine", typed("keep", 9.0F, 1));

        store.sync(initial, new BlockPos(1, 2, 3));
        store.set(initial, new BlockPos(1, 2, 3), "drop", 7.25F);
        store.sync(replacement, new BlockPos(7, 8, 9));

        SmartInterfaceData kept = store.get(replacement, new BlockPos(7, 8, 9), "keep");
        assertNotNull(kept);
        assertEquals(7, kept.getPos().getX());
        assertEquals(8, kept.getPos().getY());
        assertEquals(9, kept.getPos().getZ());
        assertEquals(replacement.getRegistryName(), kept.getParent());
        assertEquals(2.0F, kept.getValue(), 0.0001F);
        assertNull(store.get(replacement, new BlockPos(7, 8, 9), "drop"));
        assertArrayEquals(new String[] {"keep"}, types(store.list(replacement, new BlockPos(7, 8, 9))));
    }

    @Test
    public void syncRepairsPositionAndParentWithoutChangingValue() {
        VirtualSmartInterfaceStore store = new VirtualSmartInterfaceStore();
        DynamicMachine source = machine("source_machine", typed("power", 1.5F, 4));
        DynamicMachine target = machine("target_machine", typed("power", 3.5F, 4));
        BlockPos firstPos = new BlockPos(5, 6, 7);
        BlockPos secondPos = new BlockPos(-4, 32, 18);

        store.sync(source, firstPos);
        store.set(source, firstPos, "power", 12.25F);
        store.sync(target, secondPos);

        SmartInterfaceData data = store.get(target, secondPos, "power");
        assertNotNull(data);
        assertEquals(secondPos, data.getPos());
        assertEquals(target.getRegistryName(), data.getParent());
        assertEquals("power", data.getType());
        assertEquals(12.25F, data.getValue(), 0.0001F);
    }

    @Test
    public void listUsesMachineNaturalOrderBeforeRemainingStoredEntries() {
        VirtualSmartInterfaceStore store = new VirtualSmartInterfaceStore();
        DynamicMachine machine = machine(
            "ordered_machine",
            typed("low", 2.0F, 1),
            typed("high", 9.0F, 10),
            typed("mid", 5.0F, 4)
        );
        BlockPos pos = new BlockPos(1, 1, 1);

        store.sync(machine, pos);
        assertArrayEquals(new String[] {"high", "mid", "low"}, types(store.list(machine, pos)));
    }

    @Test
    public void writeAndReadRoundTripVirtualEntriesAndClearOldState() {
        VirtualSmartInterfaceStore source = new VirtualSmartInterfaceStore();
        DynamicMachine sourceMachine = machine(
            "roundtrip_source",
            typed("alpha", 1.25F, 3),
            typed("beta", 7.5F, 1)
        );
        BlockPos sourcePos = new BlockPos(2, 3, 4);
        source.sync(sourceMachine, sourcePos);
        source.set(sourceMachine, sourcePos, "alpha", 4.25F);

        NBTTagCompound compound = new NBTTagCompound();
        source.writeTo(compound);

        VirtualSmartInterfaceStore target = new VirtualSmartInterfaceStore();
        DynamicMachine staleMachine = machine("stale_machine", typed("gamma", 9.0F, 2));
        BlockPos stalePos = new BlockPos(9, 9, 9);
        target.sync(staleMachine, stalePos);
        target.set(staleMachine, stalePos, "gamma", 99.0F);

        target.readFrom(compound);
        target.sync(sourceMachine, sourcePos);

        SmartInterfaceData[] roundTripped = target.list(sourceMachine, sourcePos);
        assertEquals(2, roundTripped.length);
        assertEquals("alpha", roundTripped[0].getType());
        assertEquals(sourcePos, roundTripped[0].getPos());
        assertEquals(sourceMachine.getRegistryName(), roundTripped[0].getParent());
        assertEquals(4.25F, roundTripped[0].getValue(), 0.0001F);
        assertEquals("beta", roundTripped[1].getType());
        assertEquals(sourcePos, roundTripped[1].getPos());
        assertEquals(sourceMachine.getRegistryName(), roundTripped[1].getParent());
        assertNull(target.get(null, stalePos, "gamma"));
    }

    private static DynamicMachine machine(String name, SmartInterfaceType... types) {
        DynamicMachine machine = new DynamicMachine(name);
        for (SmartInterfaceType type : types) {
            machine.addSmartInterfaceType(type);
        }
        return machine;
    }

    private static SmartInterfaceType typed(String type, float defaultValue, int priority) {
        return new SmartInterfaceType(type, defaultValue).setPriority(priority);
    }

    private static String[] types(SmartInterfaceData[] list) {
        String[] result = new String[list.length];
        for (int i = 0; i < list.length; i++) {
            result[i] = list[i].getType();
        }
        return result;
    }
}

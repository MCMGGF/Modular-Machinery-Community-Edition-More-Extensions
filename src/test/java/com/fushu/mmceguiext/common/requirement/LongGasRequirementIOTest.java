package com.fushu.mmceguiext.common.requirement;

import github.kasuminova.mmce.common.util.IExtendedGasHandler;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTankInfo;
import net.minecraft.util.EnumFacing;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class LongGasRequirementIOTest {
    @Test
    public void longSnapshotIncludesEveryGasSlotAndDoesNotMutateSource() {
        final Gas hydrogen = new Gas("mmcege_test_hydrogen", "mmcege_test_hydrogen");
        final Gas oxygen = new Gas("mmcege_test_oxygen", "mmcege_test_oxygen");
        FakeLongGasHandler source = new FakeLongGasHandler(
            hydrogen,
            5_000_000_000L,
            oxygen,
            7_000_000_000L
        );
        MachineComponent<IExtendedGasHandler> component =
            new MachineComponent<IExtendedGasHandler>(IOType.INPUT) {
                @Override
                public ComponentType getComponentType() {
                    return ComponentTypesMM.COMPONENT_GAS;
                }

                @Override
                public IExtendedGasHandler getContainerProvider() {
                    return source;
                }
            };
        ProcessingComponent<IExtendedGasHandler> processing =
            new ProcessingComponent<IExtendedGasHandler>(component, source, null);

        List<ProcessingComponent<?>> copied = LongGasRequirementIO.copyGasComponents(
            Collections.<ProcessingComponent<?>>singletonList(processing)
        );
        IExtendedGasHandler snapshot =
            (IExtendedGasHandler) copied.get(0).getProvidedComponent();

        assertNotSame(source, snapshot);
        assertEquals(
            5_000_000_000L,
            LongGasRequirementIO.simulateGas(
                new GasStack(hydrogen, 1),
                Collections.singletonList(snapshot),
                Long.MAX_VALUE,
                IOType.INPUT
            )
        );
        assertEquals(
            7_000_000_000L,
            LongGasRequirementIO.simulateGas(
                new GasStack(oxygen, 1),
                Collections.singletonList(snapshot),
                Long.MAX_VALUE,
                IOType.INPUT
            )
        );

        LongGasRequirementIO.doGas(
            new GasStack(hydrogen, 1),
            Collections.singletonList(snapshot),
            4_000_000_000L,
            IOType.INPUT
        );

        assertEquals(5_000_000_000L, source.firstAmount);
        assertEquals(
            1_000_000_000L,
            LongGasRequirementIO.simulateGas(
                new GasStack(hydrogen, 1),
                Collections.singletonList(snapshot),
                Long.MAX_VALUE,
                IOType.INPUT
            )
        );
    }

    private static final class FakeLongGasHandler implements IExtendedGasHandler, LongGasIOHandler {
        private final Gas first;
        private final Gas second;
        private long firstAmount;
        private long secondAmount;

        private FakeLongGasHandler(Gas first, long firstAmount, Gas second, long secondAmount) {
            this.first = first;
            this.firstAmount = firstAmount;
            this.second = second;
            this.secondAmount = secondAmount;
        }

        @Nullable
        @Override
        public GasStack drawGas(GasStack toDraw, boolean doTransfer) {
            return null;
        }

        @Nullable
        @Override
        public GasStack drawGas(@Nullable EnumFacing side, int amount, boolean doTransfer) {
            return null;
        }

        @Override
        public int receiveGas(@Nullable EnumFacing side, GasStack stack, boolean doTransfer) {
            return 0;
        }

        @Override
        public boolean canReceiveGas(@Nullable EnumFacing side, Gas gas) {
            return false;
        }

        @Override
        public boolean canDrawGas(@Nullable EnumFacing side, Gas gas) {
            return gas == this.first || gas == this.second;
        }

        @Nonnull
        @Override
        public GasTankInfo[] getTankInfo() {
            return new GasTankInfo[]{
                info(this.first),
                info(this.second)
            };
        }

        @Override
        public long mmceguiext$simulateGasIO(GasStack stack, long maxAmount, IOType actionType) {
            return transfer(stack, maxAmount, actionType, false);
        }

        @Override
        public long mmceguiext$doGasIO(GasStack stack, long maxAmount, IOType actionType) {
            return transfer(stack, maxAmount, actionType, true);
        }

        private long transfer(GasStack stack, long maxAmount, IOType actionType, boolean mutate) {
            if (stack == null || actionType != IOType.INPUT) {
                return 0L;
            }
            if (stack.getGas() == this.first) {
                long moved = Math.min(maxAmount, this.firstAmount);
                if (mutate) {
                    this.firstAmount -= moved;
                }
                return moved;
            }
            if (stack.getGas() == this.second) {
                long moved = Math.min(maxAmount, this.secondAmount);
                if (mutate) {
                    this.secondAmount -= moved;
                }
                return moved;
            }
            return 0L;
        }

        private static GasTankInfo info(final Gas gas) {
            return new GasTankInfo() {
                @Nullable
                @Override
                public GasStack getGas() {
                    return new GasStack(gas, 1);
                }

                @Override
                public int getStored() {
                    return 1;
                }

                @Override
                public int getMaxGas() {
                    return Integer.MAX_VALUE;
                }
            };
        }
    }
}

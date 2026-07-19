package com.fushu.mmceguiext.common.requirement;

import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import org.junit.Test;
import org.junit.BeforeClass;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class LongRequirementIOTest {
    @BeforeClass
    public static void bootstrapMinecraftRegistries() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    public void longSnapshotIncludesEveryFluidSlotAndDoesNotMutateSource() {
        final Fluid water = testFluid("water");
        final Fluid lava = testFluid("lava");
        FakeLongFluidHandler source = new FakeLongFluidHandler(
            water,
            5_000_000_000L,
            lava,
            7_000_000_000L
        );
        MachineComponent.FluidHatch component = new MachineComponent.FluidHatch(IOType.INPUT) {
            @Override
            public IFluidHandler getContainerProvider() {
                return source;
            }
        };
        ProcessingComponent<IFluidHandler> processing =
            new ProcessingComponent<IFluidHandler>(component, source, null);

        List<ProcessingComponent<?>> copied = LongRequirementIO.copyFluidComponents(
            Collections.<ProcessingComponent<?>>singletonList(processing)
        );
        IFluidHandler snapshot = (IFluidHandler) copied.get(0).getProvidedComponent();

        assertNotSame(source, snapshot);
        assertEquals(
            5_000_000_000L,
            LongRequirementIO.simulateFluid(
                new FluidStack(water, 1),
                Collections.singletonList(snapshot),
                Long.MAX_VALUE,
                IOType.INPUT
            )
        );
        assertEquals(
            7_000_000_000L,
            LongRequirementIO.simulateFluid(
                new FluidStack(lava, 1),
                Collections.singletonList(snapshot),
                Long.MAX_VALUE,
                IOType.INPUT
            )
        );

        LongRequirementIO.doFluid(
            new FluidStack(water, 1),
            Collections.singletonList(snapshot),
            4_000_000_000L,
            IOType.INPUT
        );

        assertEquals(5_000_000_000L, source.firstAmount);
        assertEquals(
            1_000_000_000L,
            LongRequirementIO.simulateFluid(
                new FluidStack(water, 1),
                Collections.singletonList(snapshot),
                Long.MAX_VALUE,
                IOType.INPUT
            )
        );
    }

    @Test
    public void ordinaryMultiTankSnapshotCanContributeMoreThanIntegerMax() {
        final Fluid water = testFluid("multi_tank_water");
        IFluidHandler source = new FakeIntMultiTankHandler(water, 1_500_000_000, 1_500_000_000);
        MachineComponent.FluidHatch component = new MachineComponent.FluidHatch(IOType.INPUT) {
            @Override
            public IFluidHandler getContainerProvider() {
                return source;
            }
        };
        ProcessingComponent<IFluidHandler> processing =
            new ProcessingComponent<IFluidHandler>(component, source, null);
        IFluidHandler snapshot = (IFluidHandler) LongRequirementIO.copyFluidComponents(
            Collections.<ProcessingComponent<?>>singletonList(processing)
        ).get(0).getProvidedComponent();

        assertEquals(
            3_000_000_000L,
            LongRequirementIO.simulateFluid(
                new FluidStack(water, 1),
                Collections.singletonList(snapshot),
                Long.MAX_VALUE,
                IOType.INPUT
            )
        );
    }

    private static Fluid testFluid(String name) {
        ResourceLocation texture = new ResourceLocation("mmceguiext", "test/" + name);
        Fluid fluid = new Fluid("mmcege_test_" + name, texture, texture);
        FluidRegistry.registerFluid(fluid);
        return FluidRegistry.getFluid(fluid.getName());
    }

    private static final class FakeLongFluidHandler implements IFluidHandler, LongFluidIOHandler {
        private final Fluid first;
        private final Fluid second;
        private long firstAmount;
        private long secondAmount;

        private FakeLongFluidHandler(Fluid first, long firstAmount, Fluid second, long secondAmount) {
            this.first = first;
            this.firstAmount = firstAmount;
            this.second = second;
            this.secondAmount = secondAmount;
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            return new IFluidTankProperties[]{
                property(this.first),
                property(this.second)
            };
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return 0;
        }

        @Nullable
        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            return null;
        }

        @Nullable
        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return null;
        }

        @Override
        public long mmceguiext$simulateFluidIO(FluidStack stack, long maxAmount, IOType actionType) {
            return transfer(stack, maxAmount, actionType, false);
        }

        @Override
        public long mmceguiext$doFluidIO(FluidStack stack, long maxAmount, IOType actionType) {
            return transfer(stack, maxAmount, actionType, true);
        }

        private long transfer(FluidStack stack, long maxAmount, IOType actionType, boolean mutate) {
            if (stack == null || actionType != IOType.INPUT) {
                return 0L;
            }
            if (stack.getFluid() == this.first) {
                long moved = Math.min(maxAmount, this.firstAmount);
                if (mutate) {
                    this.firstAmount -= moved;
                }
                return moved;
            }
            if (stack.getFluid() == this.second) {
                long moved = Math.min(maxAmount, this.secondAmount);
                if (mutate) {
                    this.secondAmount -= moved;
                }
                return moved;
            }
            return 0L;
        }

        private static IFluidTankProperties property(final Fluid fluid) {
            return new IFluidTankProperties() {
                @Nullable
                @Override
                public FluidStack getContents() {
                    return new FluidStack(fluid, 1);
                }

                @Override
                public int getCapacity() {
                    return Integer.MAX_VALUE;
                }

                @Override
                public boolean canFill() {
                    return true;
                }

                @Override
                public boolean canDrain() {
                    return true;
                }

                @Override
                public boolean canFillFluidType(FluidStack fluidStack) {
                    return fluidStack != null && fluidStack.getFluid() == fluid;
                }

                @Override
                public boolean canDrainFluidType(FluidStack fluidStack) {
                    return fluidStack != null && fluidStack.getFluid() == fluid;
                }
            };
        }
    }

    private static final class FakeIntMultiTankHandler implements IFluidHandler {
        private final Fluid fluid;
        private final int[] amounts;

        private FakeIntMultiTankHandler(Fluid fluid, int... amounts) {
            this.fluid = fluid;
            this.amounts = amounts;
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            IFluidTankProperties[] properties = new IFluidTankProperties[this.amounts.length];
            for (int i = 0; i < this.amounts.length; i++) {
                final int amount = this.amounts[i];
                properties[i] = new IFluidTankProperties() {
                    @Nullable
                    @Override
                    public FluidStack getContents() {
                        return new FluidStack(FakeIntMultiTankHandler.this.fluid, amount);
                    }

                    @Override
                    public int getCapacity() {
                        return amount;
                    }

                    @Override
                    public boolean canFill() {
                        return true;
                    }

                    @Override
                    public boolean canDrain() {
                        return true;
                    }

                    @Override
                    public boolean canFillFluidType(FluidStack fluidStack) {
                        return fluidStack != null
                            && fluidStack.getFluid() == FakeIntMultiTankHandler.this.fluid;
                    }

                    @Override
                    public boolean canDrainFluidType(FluidStack fluidStack) {
                        return canFillFluidType(fluidStack);
                    }
                };
            }
            return properties;
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return 0;
        }

        @Nullable
        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            return null;
        }

        @Nullable
        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return null;
        }
    }
}

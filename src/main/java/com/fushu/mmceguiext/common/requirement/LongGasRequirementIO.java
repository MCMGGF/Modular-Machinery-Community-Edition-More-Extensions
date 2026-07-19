package com.fushu.mmceguiext.common.requirement;

import github.kasuminova.mmce.common.util.IExtendedGasHandler;
import github.kasuminova.mmce.common.util.MultiGasTank;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.IGasHandler;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Optional.Interface(modid = "mekanism", iface = "mekanism.api.gas.IGasHandler")
public final class LongGasRequirementIO {
    private LongGasRequirementIO() {
    }

    @Optional.Method(modid = "mekanism")
    public static long simulateGas(GasStack stack, List<IExtendedGasHandler> handlers, long maxAmount, IOType actionType) {
        if (stack == null || handlers == null || maxAmount <= 0L) {
            return 0L;
        }
        long total = 0L;
        for (IExtendedGasHandler handler : handlers) {
            if (handler == null) {
                continue;
            }
            long remaining = maxAmount - total;
            if (remaining <= 0L) {
                break;
            }
            if (handler instanceof LongGasIOHandler) {
                total += clampMoved(((LongGasIOHandler) handler).mmceguiext$simulateGasIO(stack, remaining, actionType), remaining);
            } else {
                total += simulateGasFallback(stack, handler, remaining, actionType);
            }
        }
        return total;
    }

    @Optional.Method(modid = "mekanism")
    public static void doGas(GasStack stack, List<IExtendedGasHandler> handlers, long maxAmount, IOType actionType) {
        if (stack == null || handlers == null || maxAmount <= 0L) {
            return;
        }
        long remaining = maxAmount;
        for (IExtendedGasHandler handler : handlers) {
            if (handler == null || remaining <= 0L) {
                continue;
            }
            long moved = 0L;
            if (handler instanceof LongGasIOHandler) {
                moved = clampMoved(((LongGasIOHandler) handler).mmceguiext$doGasIO(stack, remaining, actionType), remaining);
            } else {
                moved = doGasFallback(stack, handler, remaining, actionType);
            }
            remaining -= clampMoved(moved, remaining);
        }
    }

    public static boolean hasLongGasHandler(List<ProcessingComponent<?>> components) {
        if (components == null) {
            return false;
        }
        for (ProcessingComponent<?> component : components) {
            Object provided = component == null ? null : component.getProvidedComponent();
            if (provided instanceof LongGasIOHandler) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Optional.Method(modid = "mekanism")
    public static List<ProcessingComponent<?>> copyGasComponents(List<ProcessingComponent<?>> components) {
        List<ProcessingComponent<?>> out = new ArrayList<ProcessingComponent<?>>();
        if (components == null) {
            return out;
        }
        for (ProcessingComponent<?> component : components) {
            if (component == null) {
                continue;
            }
            Object provided = component.getProvidedComponent();
            if (provided instanceof LongGasIOHandler && provided instanceof IExtendedGasHandler) {
                out.add(new ProcessingComponent((MachineComponent) component.component(), new SnapshotGasHandler((IExtendedGasHandler) provided, (LongGasIOHandler) provided), component.getTag()));
            } else if (provided instanceof IGasHandler) {
                out.add(new ProcessingComponent((MachineComponent) component.component(), new IntSnapshotGasHandler((IGasHandler) provided), component.getTag()));
            }
        }
        return out;
    }

    private static int downcastAmount(long value) {
        return LongRequirementAmounts.downcastAmount(value);
    }

    private static long clampMoved(long moved, long maxAmount) {
        if (moved <= 0L || maxAmount <= 0L) {
            return 0L;
        }
        return Math.min(moved, maxAmount);
    }

    @Optional.Method(modid = "mekanism")
    private static long simulateGasFallback(GasStack stack, IExtendedGasHandler handler, long maxAmount, IOType actionType) {
        return gasFallback(stack, handler, maxAmount, actionType, false);
    }

    @Optional.Method(modid = "mekanism")
    private static long doGasFallback(GasStack stack, IExtendedGasHandler handler, long maxAmount, IOType actionType) {
        return gasFallback(stack, handler, maxAmount, actionType, true);
    }

    @Optional.Method(modid = "mekanism")
    private static long gasFallback(GasStack stack, IExtendedGasHandler handler, long maxAmount, IOType actionType, boolean doTransfer) {
        if (stack == null || handler == null || maxAmount <= 0L) {
            return 0L;
        }
        long moved = 0L;
        while (moved < maxAmount) {
            long remaining = maxAmount - moved;
            GasStack copy = stack.copy();
            copy.amount = downcastAmount(remaining);
            long step = 0L;
            if (actionType == IOType.INPUT) {
                GasStack drawn = handler.drawGas(copy, doTransfer);
                step = drawn == null ? 0L : Math.max(0L, drawn.amount);
            } else if (handler.canReceiveGas(null, copy.getGas())) {
                step = Math.max(0, handler.receiveGas(null, copy, doTransfer));
            }
            step = clampMoved(step, remaining);
            if (step <= 0L) {
                break;
            }
            moved += step;
            if (!doTransfer || step < copy.amount) {
                break;
            }
        }
        return moved;
    }

    private static final class IntSnapshotGasHandler implements IExtendedGasHandler, LongGasIOHandler {
        private final MultiGasTank delegate;

        private IntSnapshotGasHandler(IGasHandler source) {
            this.delegate = new MultiGasTank(source);
        }

        @Nullable
        @Override
        @Optional.Method(modid = "mekanism")
        public GasStack drawGas(GasStack toDraw, boolean doTransfer) {
            return this.delegate.drawGas(toDraw, doTransfer);
        }

        @Nullable
        @Override
        @Optional.Method(modid = "mekanism")
        public GasStack drawGas(@Nullable net.minecraft.util.EnumFacing side, int amount, boolean doTransfer) {
            return this.delegate.drawGas(side, amount, doTransfer);
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public int receiveGas(@Nullable net.minecraft.util.EnumFacing side, GasStack stack, boolean doTransfer) {
            return this.delegate.receiveGas(side, stack, doTransfer);
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public boolean canReceiveGas(@Nullable net.minecraft.util.EnumFacing side, Gas gas) {
            return this.delegate.canReceiveGas(side, gas);
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public boolean canDrawGas(@Nullable net.minecraft.util.EnumFacing side, Gas gas) {
            return this.delegate.canDrawGas(side, gas);
        }

        @Nonnull
        @Override
        @Optional.Method(modid = "mekanism")
        public GasTankInfo[] getTankInfo() {
            return this.delegate.getTankInfo();
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public long mmceguiext$simulateGasIO(GasStack stack, long maxAmount, IOType actionType) {
            return gasFallback(stack, new MultiGasTank(this.delegate), maxAmount, actionType, true);
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public long mmceguiext$doGasIO(GasStack stack, long maxAmount, IOType actionType) {
            return gasFallback(stack, this.delegate, maxAmount, actionType, true);
        }
    }

    private static final class SnapshotGasHandler implements IExtendedGasHandler, LongGasIOHandler {
        private final LongGasIOHandler source;
        private final List<GasState> states = new ArrayList<GasState>();

        private SnapshotGasHandler(IExtendedGasHandler handler, LongGasIOHandler source) {
            this.source = source;
            GasTankInfo[] infos = handler.getTankInfo();
            if (infos == null) {
                return;
            }
            for (GasTankInfo info : infos) {
                if (info == null) {
                    continue;
                }
                GasStack contents = info.getGas();
                if (contents != null && contents.amount > 0) {
                    getOrCreateState(contents);
                }
            }
        }

        @Nullable
        @Override
        @Optional.Method(modid = "mekanism")
        public GasStack drawGas(GasStack toDraw, boolean doTransfer) {
            if (toDraw == null) {
                return null;
            }
            long moved = doGas(toDraw, toDraw.amount, IOType.INPUT, doTransfer);
            if (moved <= 0L) {
                return null;
            }
            GasStack out = toDraw.copy();
            out.amount = downcastAmount(moved);
            return out;
        }

        @Nullable
        @Override
        @Optional.Method(modid = "mekanism")
        public GasStack drawGas(@Nullable net.minecraft.util.EnumFacing side, int amount, boolean doTransfer) {
            for (GasState state : this.states) {
                if (state.amount <= 0L) {
                    continue;
                }
                long moved = doGas(state.prototype, amount, IOType.INPUT, doTransfer);
                if (moved > 0L) {
                    GasStack out = state.prototype.copy();
                    out.amount = downcastAmount(moved);
                    return out;
                }
            }
            return null;
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public int receiveGas(@Nullable net.minecraft.util.EnumFacing side, GasStack stack, boolean doTransfer) {
            return downcastAmount(doGas(stack, stack == null ? 0L : stack.amount, IOType.OUTPUT, doTransfer));
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public boolean canReceiveGas(@Nullable net.minecraft.util.EnumFacing side, Gas gas) {
            return gas != null && doGas(new GasStack(gas, 1), 1L, IOType.OUTPUT, false) > 0L;
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public boolean canDrawGas(@Nullable net.minecraft.util.EnumFacing side, Gas gas) {
            if (gas == null) {
                return false;
            }
            for (GasState state : this.states) {
                if (state.amount > 0L && state.prototype.getGas() == gas) {
                    return true;
                }
            }
            return false;
        }

        @Nonnull
        @Override
        @Optional.Method(modid = "mekanism")
        public GasTankInfo[] getTankInfo() {
            GasTankInfo[] info = new GasTankInfo[this.states.size()];
            for (int i = 0; i < this.states.size(); i++) {
                info[i] = new SnapshotGasTankInfo(this.states.get(i));
            }
            return info;
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public long mmceguiext$simulateGasIO(GasStack stack, long maxAmount, IOType actionType) {
            return doGas(stack, maxAmount, actionType, false);
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public long mmceguiext$doGasIO(GasStack stack, long maxAmount, IOType actionType) {
            return doGas(stack, maxAmount, actionType, true);
        }

        @Optional.Method(modid = "mekanism")
        private long doGas(GasStack stack, long maxAmount, IOType actionType, boolean mutate) {
            if (stack == null || maxAmount <= 0L) {
                return 0L;
            }
            GasState state = getOrCreateState(stack);
            if (actionType == IOType.INPUT) {
                if (state.amount <= 0L) {
                    return 0L;
                }
                long moved = Math.min(maxAmount, state.amount);
                if (mutate) {
                    state.amount -= moved;
                }
                return moved;
            }
            long moved = Math.min(maxAmount, Math.max(0L, state.capacity - state.amount));
            if (mutate) {
                state.amount += moved;
            }
            return moved;
        }

        @Optional.Method(modid = "mekanism")
        private GasState getOrCreateState(GasStack stack) {
            for (GasState state : this.states) {
                if (state.prototype.isGasEqual(stack)) {
                    return state;
                }
            }
            GasStack prototype = stack.copy();
            prototype.amount = 1;
            long amount = clampMoved(
                this.source.mmceguiext$simulateGasIO(prototype, Long.MAX_VALUE, IOType.INPUT),
                Long.MAX_VALUE
            );
            long outputLimit = Long.MAX_VALUE - amount;
            long free = clampMoved(
                this.source.mmceguiext$simulateGasIO(prototype, outputLimit, IOType.OUTPUT),
                outputLimit
            );
            GasState state = new GasState(
                prototype,
                amount,
                LongRequirementAmounts.saturatedAdd(amount, free)
            );
            this.states.add(state);
            return state;
        }

        private static final class GasState {
            private final GasStack prototype;
            private long amount;
            private final long capacity;

            private GasState(GasStack prototype, long amount, long capacity) {
                this.prototype = prototype;
                this.amount = amount;
                this.capacity = capacity;
            }
        }

        private final class SnapshotGasTankInfo implements GasTankInfo {
            private final GasState state;

            private SnapshotGasTankInfo(GasState state) {
                this.state = state;
            }

            @Nullable
            @Override
            @Optional.Method(modid = "mekanism")
            public GasStack getGas() {
                if (this.state.amount <= 0L) {
                    return null;
                }
                GasStack out = this.state.prototype.copy();
                out.amount = downcastAmount(this.state.amount);
                return out;
            }

            @Override
            @Optional.Method(modid = "mekanism")
            public int getStored() {
                return downcastAmount(this.state.amount);
            }

            @Override
            @Optional.Method(modid = "mekanism")
            public int getMaxGas() {
                return downcastAmount(this.state.capacity);
            }
        }
    }
}

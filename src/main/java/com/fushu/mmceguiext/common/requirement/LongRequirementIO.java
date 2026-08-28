package com.fushu.mmceguiext.common.requirement;

import github.kasuminova.mmce.common.util.IOneToOneFluidHandler;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class LongRequirementIO {
    private LongRequirementIO() {
    }

    public static long simulateFluid(FluidStack stack, List<IFluidHandler> handlers, long maxAmount, IOType actionType) {
        if (stack == null || handlers == null || maxAmount <= 0L) {
            return 0L;
        }
        long total = 0L;
        for (IFluidHandler handler : handlers) {
            if (handler == null) {
                continue;
            }
            long remaining = maxAmount - total;
            if (remaining <= 0L) {
                break;
            }
            if (handler instanceof LongFluidIOHandler) {
                total += clampMoved(((LongFluidIOHandler) handler).mmceguiext$simulateFluidIO(stack, remaining, actionType), remaining);
            } else {
                total += simulateFluidFallback(stack, handler, remaining, actionType);
            }
        }
        return total;
    }

    public static void doFluid(FluidStack stack, List<IFluidHandler> handlers, long maxAmount, IOType actionType) {
        if (stack == null || handlers == null || maxAmount <= 0L) {
            return;
        }
        long remaining = maxAmount;
        for (IFluidHandler handler : handlers) {
            if (handler == null || remaining <= 0L) {
                continue;
            }
            long moved;
            if (handler instanceof LongFluidIOHandler) {
                moved = clampMoved(((LongFluidIOHandler) handler).mmceguiext$doFluidIO(stack, remaining, actionType), remaining);
            } else {
                moved = doFluidFallback(stack, handler, remaining, actionType);
            }
            remaining -= clampMoved(moved, remaining);
        }
    }

    public static boolean hasLongFluidHandler(List<ProcessingComponent<?>> components) {
        if (components == null) {
            return false;
        }
        for (ProcessingComponent<?> component : components) {
            Object provided = component == null ? null : component.getProvidedComponent();
            if (provided instanceof LongFluidIOHandler) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List<ProcessingComponent<?>> copyFluidComponents(List<ProcessingComponent<?>> components) {
        List<ProcessingComponent<?>> out = new ArrayList<ProcessingComponent<?>>();
        if (components == null) {
            return out;
        }
        for (ProcessingComponent<?> component : components) {
            if (component == null) {
                continue;
            }
            Object provided = component.getProvidedComponent();
            if (provided instanceof LongFluidIOHandler && provided instanceof IFluidHandler) {
                out.add(new ProcessingComponent((MachineComponent) component.component(), new SnapshotFluidHandler((IFluidHandler) provided, (LongFluidIOHandler) provided), component.getTag()));
            } else if (provided instanceof IFluidHandler) {
                out.add(new ProcessingComponent((MachineComponent) component.component(), new IntSnapshotFluidHandler((IFluidHandler) provided), component.getTag()));
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

    private static long simulateFluidFallback(FluidStack stack, IFluidHandler handler, long maxAmount, IOType actionType) {
        return fluidFallback(stack, handler, maxAmount, actionType, false);
    }

    private static long doFluidFallback(FluidStack stack, IFluidHandler handler, long maxAmount, IOType actionType) {
        return fluidFallback(stack, handler, maxAmount, actionType, true);
    }

    private static long fluidFallback(FluidStack stack, IFluidHandler handler, long maxAmount, IOType actionType, boolean doTransfer) {
        if (stack == null || handler == null || maxAmount <= 0L) {
            return 0L;
        }
        long moved = 0L;
        while (moved < maxAmount) {
            long remaining = maxAmount - moved;
            FluidStack copy = stack.copy();
            copy.amount = downcastAmount(remaining);
            long step;
            if (actionType == IOType.INPUT) {
                FluidStack drained = handler.drain(copy, doTransfer);
                step = drained == null ? 0L : Math.max(0L, drained.amount);
            } else {
                step = Math.max(0, handler.fill(copy, doTransfer));
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

    private static final class IntSnapshotFluidHandler implements IFluidHandler, LongFluidIOHandler {
        private final List<FluidState> states = new ArrayList<FluidState>();
        private final boolean oneFluidOneSlot;

        private IntSnapshotFluidHandler(IFluidHandler source) {
            IFluidTankProperties[] properties = source == null ? null : source.getTankProperties();
            this.oneFluidOneSlot = source instanceof IOneToOneFluidHandler
                && ((IOneToOneFluidHandler) source).isOneFluidOneSlot();
            if (properties == null) {
                return;
            }
            for (IFluidTankProperties property : properties) {
                if (property == null) {
                    continue;
                }
                FluidStack contents = property.getContents();
                FluidStack prototype = contents == null ? null : contents.copy();
                if (prototype != null) {
                    prototype.amount = 1;
                }
                this.states.add(new FluidState(
                    prototype,
                    contents == null ? 0L : Math.max(0L, contents.amount),
                    Math.max(0L, property.getCapacity())
                ));
            }
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            IFluidTankProperties[] properties = new IFluidTankProperties[this.states.size()];
            for (int i = 0; i < this.states.size(); i++) {
                properties[i] = new SnapshotFluidTankProperties(this.states.get(i));
            }
            return properties;
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return downcastAmount(doFluid(resource, resource == null ? 0L : resource.amount, IOType.OUTPUT, doFill));
        }

        @Nullable
        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (resource == null) {
                return null;
            }
            long moved = doFluid(resource, resource.amount, IOType.INPUT, doDrain);
            if (moved <= 0L) {
                return null;
            }
            FluidStack result = resource.copy();
            result.amount = downcastAmount(moved);
            return result;
        }

        @Nullable
        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            if (maxDrain <= 0) {
                return null;
            }
            for (FluidState state : this.states) {
                if (state.prototype == null || state.amount <= 0L) {
                    continue;
                }
                long moved = doFluid(state.prototype, maxDrain, IOType.INPUT, doDrain);
                if (moved > 0L) {
                    FluidStack result = state.prototype.copy();
                    result.amount = downcastAmount(moved);
                    return result;
                }
            }
            return null;
        }

        @Override
        public long mmceguiext$simulateFluidIO(FluidStack stack, long maxAmount, IOType actionType) {
            return doFluid(stack, maxAmount, actionType, false);
        }

        @Override
        public long mmceguiext$doFluidIO(FluidStack stack, long maxAmount, IOType actionType) {
            return doFluid(stack, maxAmount, actionType, true);
        }

        private long doFluid(FluidStack stack, long maxAmount, IOType actionType, boolean mutate) {
            if (stack == null || maxAmount <= 0L) {
                return 0L;
            }
            long movedTotal = 0L;
            boolean foundMatchingState = false;
            for (FluidState state : this.states) {
                if (state.prototype != null && state.prototype.isFluidEqual(stack)) {
                    foundMatchingState = true;
                    break;
                }
            }
            for (FluidState state : this.states) {
                if (movedTotal >= maxAmount) {
                    break;
                }
                long remaining = maxAmount - movedTotal;
                if (actionType == IOType.INPUT) {
                    if (state.prototype == null || !state.prototype.isFluidEqual(stack) || state.amount <= 0L) {
                        continue;
                    }
                    long moved = Math.min(remaining, state.amount);
                    if (mutate) {
                        state.amount -= moved;
                        if (state.amount == 0L) {
                            state.prototype = null;
                        }
                    }
                    movedTotal += moved;
                    continue;
                }

                if (state.prototype != null && !state.prototype.isFluidEqual(stack)) {
                    continue;
                }
                if (this.oneFluidOneSlot && foundMatchingState
                    && (state.prototype == null || !state.prototype.isFluidEqual(stack))) {
                    continue;
                }
                long free = Math.max(0L, state.capacity - state.amount);
                if (free <= 0L) {
                    continue;
                }
                long moved = Math.min(remaining, free);
                if (mutate) {
                    if (state.prototype == null) {
                        state.prototype = stack.copy();
                        state.prototype.amount = 1;
                    }
                    state.amount += moved;
                }
                movedTotal += moved;
                if (this.oneFluidOneSlot) {
                    break;
                }
            }
            return movedTotal;
        }

        private static final class FluidState {
            private FluidStack prototype;
            private long amount;
            private final long capacity;

            private FluidState(FluidStack prototype, long amount, long capacity) {
                this.prototype = prototype;
                this.amount = amount;
                this.capacity = capacity;
            }
        }

        private static final class SnapshotFluidTankProperties implements IFluidTankProperties {
            private final FluidState state;

            private SnapshotFluidTankProperties(FluidState state) {
                this.state = state;
            }

            @Nullable
            @Override
            public FluidStack getContents() {
                if (this.state.prototype == null || this.state.amount <= 0L) {
                    return null;
                }
                FluidStack result = this.state.prototype.copy();
                result.amount = downcastAmount(this.state.amount);
                return result;
            }

            @Override
            public int getCapacity() {
                return downcastAmount(this.state.capacity);
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
                    && (this.state.prototype == null || this.state.prototype.isFluidEqual(fluidStack));
            }

            @Override
            public boolean canDrainFluidType(FluidStack fluidStack) {
                return fluidStack != null
                    && this.state.prototype != null
                    && this.state.prototype.isFluidEqual(fluidStack);
            }
        }
    }

    private static final class SnapshotFluidHandler implements IFluidHandler, LongFluidIOHandler {
        private final LongFluidIOHandler source;
        private final List<FluidState> states = new ArrayList<FluidState>();

        private SnapshotFluidHandler(IFluidHandler handler, LongFluidIOHandler source) {
            this.source = source;
            IFluidTankProperties[] properties = handler.getTankProperties();
            if (properties == null) {
                return;
            }
            for (IFluidTankProperties property : properties) {
                if (property == null) {
                    continue;
                }
                FluidStack contents = property.getContents();
                if (contents != null && contents.amount > 0) {
                    getOrCreateState(contents);
                }
            }
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            IFluidTankProperties[] properties = new IFluidTankProperties[this.states.size()];
            for (int i = 0; i < this.states.size(); i++) {
                properties[i] = new SnapshotFluidTankProperties(this.states.get(i));
            }
            return properties;
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return downcastAmount(doFluid(resource, resource == null ? 0L : resource.amount, IOType.OUTPUT, doFill));
        }

        @Nullable
        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (resource == null) {
                return null;
            }
            long moved = doFluid(resource, resource.amount, IOType.INPUT, doDrain);
            if (moved <= 0L) {
                return null;
            }
            FluidStack out = resource.copy();
            out.amount = downcastAmount(moved);
            return out;
        }

        @Nullable
        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            for (FluidState state : this.states) {
                if (state.amount <= 0L) {
                    continue;
                }
                long moved = doFluid(state.prototype, maxDrain, IOType.INPUT, doDrain);
                if (moved > 0L) {
                    FluidStack out = state.prototype.copy();
                    out.amount = downcastAmount(moved);
                    return out;
                }
            }
            return null;
        }

        @Override
        public long mmceguiext$simulateFluidIO(FluidStack stack, long maxAmount, IOType actionType) {
            return doFluid(stack, maxAmount, actionType, false);
        }

        @Override
        public long mmceguiext$doFluidIO(FluidStack stack, long maxAmount, IOType actionType) {
            return doFluid(stack, maxAmount, actionType, true);
        }

        private long doFluid(FluidStack stack, long maxAmount, IOType actionType, boolean mutate) {
            if (stack == null || maxAmount <= 0L) {
                return 0L;
            }
            FluidState state = getOrCreateState(stack);
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

        private FluidState getOrCreateState(FluidStack stack) {
            for (FluidState state : this.states) {
                if (state.prototype.isFluidEqual(stack)) {
                    return state;
                }
            }
            FluidStack prototype = stack.copy();
            prototype.amount = 1;
            long amount = clampMoved(
                this.source.mmceguiext$simulateFluidIO(prototype, Long.MAX_VALUE, IOType.INPUT),
                Long.MAX_VALUE
            );
            long outputLimit = Long.MAX_VALUE - amount;
            long free = clampMoved(
                this.source.mmceguiext$simulateFluidIO(prototype, outputLimit, IOType.OUTPUT),
                outputLimit
            );
            FluidState state = new FluidState(
                prototype,
                amount,
                LongRequirementAmounts.saturatedAdd(amount, free)
            );
            this.states.add(state);
            return state;
        }

        private static final class FluidState {
            private final FluidStack prototype;
            private long amount;
            private final long capacity;

            private FluidState(FluidStack prototype, long amount, long capacity) {
                this.prototype = prototype;
                this.amount = amount;
                this.capacity = capacity;
            }
        }

        private final class SnapshotFluidTankProperties implements IFluidTankProperties {
            private final FluidState state;

            private SnapshotFluidTankProperties(FluidState state) {
                this.state = state;
            }

            @Nullable
            @Override
            public FluidStack getContents() {
                if (this.state.amount <= 0L) {
                    return null;
                }
                FluidStack out = this.state.prototype.copy();
                out.amount = downcastAmount(this.state.amount);
                return out;
            }

            @Override
            public int getCapacity() {
                return downcastAmount(this.state.capacity);
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
                return fluidStack != null && this.state.prototype.isFluidEqual(fluidStack);
            }

            @Override
            public boolean canDrainFluidType(FluidStack fluidStack) {
                return fluidStack != null && this.state.prototype.isFluidEqual(fluidStack);
            }
        }
    }
}

package com.fushu.mmceguiext.mixin;

import com.fushu.mmceguiext.common.util.MultiMachineComponentProviderSupport;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.tiles.TileParallelController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import github.kasuminova.mmce.common.world.MachineComponentManager;
import github.kasuminova.mmce.common.util.InfItemFluidHandler;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = TileMultiblockMachineController.class)
public abstract class MixinTileMultiblockMachineController {

    @Shadow(remap = false)
    protected abstract void checkAndAddSmartInterface(MachineComponent<?> component, BlockPos realPos);

    @Shadow(remap = false)
    public abstract void checkAndAddUpgradeBus(MachineComponent<?> component);

    @Shadow(remap = false)
    protected TaggedPositionBlockArray foundPattern;

    @Shadow(remap = false)
    protected TileMultiblockMachineController.WorkMode workMode;

    @Final
    @Shadow(remap = false)
    protected Map<TileEntity, ProcessingComponent<?>> generalComponents;

    @Final
    @Shadow(remap = false)
    protected Set<InfItemFluidHandler> generalComponentSet;

    @Final
    @Shadow(remap = false)
    protected Long2ObjectMap<Set<InfItemFluidHandler>> componentSet;

    @Final
    @Shadow(remap = false)
    protected List<TileParallelController.ParallelControllerProvider> foundParallelControllers;

    @Inject(method = "checkAndAddComponents", at = @At("HEAD"), cancellable = true, remap = false)
    private void mmceguiext$mergeCustomMixedInputComponents(final BlockPos pos,
                                                            final BlockPos ctrlPos,
                                                            final Map<Long, Map<TileEntity, ProcessingComponent<?>>> found,
                                                            final CallbackInfo ci) {
        TileMultiblockMachineController self = (TileMultiblockMachineController) (Object) this;
        BlockPos realPos = ctrlPos.add(pos);
        if (!self.getWorld().isBlockLoaded(realPos)) {
            return;
        }

        TileEntity te = self.getWorld().getTileEntity(realPos);
        Collection<MachineComponent<?>> rawComponents = MultiMachineComponentProviderSupport.resolveMachineComponents(te);
        if (rawComponents.isEmpty()) {
            return;
        }

        List<MachineComponent<?>> components = new ObjectArrayList<MachineComponent<?>>(rawComponents);
        if (components.isEmpty()) {
            return;
        }

        ComponentSelectorTag tag = mmceguiext$readTag(pos);
        long mergedGroupId = MultiMachineComponentProviderSupport.resolveStableGroupId(te, components);

        int index = 0;
        for (MachineComponent<?> component : components) {
            if (!component.isAsyncSupported()) {
                workMode = TileMultiblockMachineController.WorkMode.SEMI_SYNC;
            }
            TileEntity key = new VirtualComponentTile(te, index++);
            mmceguiext$addProvidedComponent(component, tag, te, key, found, mergedGroupId);
            if (component instanceof TileParallelController.ParallelControllerProvider) {
                foundParallelControllers.add((TileParallelController.ParallelControllerProvider) component);
                break;
            }
            checkAndAddUpgradeBus(component);
            checkAndAddSmartInterface(component, realPos);
        }
        ci.cancel();
    }

    @Unique
    private <T> void mmceguiext$addProvidedComponent(final MachineComponent<T> component,
                                                     final ComponentSelectorTag tag,
                                                     final TileEntity owner,
                                                     final TileEntity key,
                                                     final Map<Long, Map<TileEntity, ProcessingComponent<?>>> found,
                                                     final long mergedGroupId) {
        T handler = component.getContainerProvider();
        if (handler instanceof InfItemFluidHandler
            && !mmceguiext$registerCombinedHandler((InfItemFluidHandler) handler, mergedGroupId)) {
            return;
        }

        MachineComponentManager.INSTANCE.checkComponentShared(owner, (TileMultiblockMachineController) (Object) this);
        ProcessingComponent<T> processing = new ProcessingComponent<T>(component, handler, tag);
        if (mergedGroupId < 0L) {
            generalComponents.put(key, processing);
            return;
        }

        Map<TileEntity, ProcessingComponent<?>> group = found.get(Long.valueOf(mergedGroupId));
        if (group == null) {
            group = new ConcurrentHashMap<TileEntity, ProcessingComponent<?>>();
            found.put(Long.valueOf(mergedGroupId), group);
        }
        group.put(key, processing);
    }

    @Unique
    private boolean mmceguiext$registerCombinedHandler(final InfItemFluidHandler handler, final long groupId) {
        if (groupId < 0L) {
            return generalComponentSet.add(handler);
        }
        Set<InfItemFluidHandler> handlers = componentSet.get(groupId);
        if (handlers == null) {
            handlers = new ObjectOpenHashSet<InfItemFluidHandler>();
            componentSet.put(groupId, handlers);
        }
        return handlers.add(handler);
    }

    @Unique
    private ComponentSelectorTag mmceguiext$readTag(final BlockPos pos) {
        return foundPattern == null ? null : foundPattern.getTag(pos);
    }

    @Unique
    private static final class VirtualComponentTile extends TileEntity {
        private final TileEntity delegate;
        private final int index;

        private VirtualComponentTile(final TileEntity delegate, final int index) {
            this.delegate = delegate;
            this.index = index;
            this.setWorld(delegate.getWorld());
            this.setPos(delegate.getPos());
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof VirtualComponentTile)) {
                return false;
            }
            VirtualComponentTile other = (VirtualComponentTile) obj;
            return this.index == other.index && this.delegate == other.delegate;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this.delegate) * 31 + this.index;
        }
    }
}

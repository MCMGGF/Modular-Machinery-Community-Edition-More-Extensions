package com.fushu.mmceguiext.mixin;

import com.fushu.mmceguiext.common.util.MultiMachineComponentProviderSupport;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import github.kasuminova.mmce.common.world.MachineComponentManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Shadow(remap = false)
    protected Map<TileEntity, ProcessingComponent<?>> generalComponents;

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
        Map<TileEntity, ProcessingComponent<?>> merged;
        if (mergedGroupId < 0L) {
            merged = generalComponents;
        } else {
            merged = found.get(mergedGroupId);
            if (merged == null) {
                merged = new LinkedHashMap<TileEntity, ProcessingComponent<?>>();
                found.put(mergedGroupId, merged);
            }
        }

        MachineComponentManager.INSTANCE.checkComponentShared(te, self);

        int index = 0;
        for (MachineComponent<?> component : components) {
            if (!component.isAsyncSupported()) {
                workMode = TileMultiblockMachineController.WorkMode.SEMI_SYNC;
            }
            TileEntity key = new VirtualComponentTile(te, index++);
            merged.put(key, new ProcessingComponent(component, component.getContainerProvider(), tag));
            checkAndAddUpgradeBus(component);
            checkAndAddSmartInterface(component, realPos);
        }
        ci.cancel();
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

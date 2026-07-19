package com.fushu.mmceguiext.common.util;

import net.minecraft.tileentity.TileEntity;

/**
 * Identity key for multiple logical machine components supplied by one physical tile.
 */
public final class VirtualMachineComponentTile extends TileEntity {
    private final TileEntity delegate;
    private final int index;

    public VirtualMachineComponentTile(final TileEntity delegate, final int index) {
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
        if (!(obj instanceof VirtualMachineComponentTile)) {
            return false;
        }
        VirtualMachineComponentTile other = (VirtualMachineComponentTile) obj;
        return this.index == other.index && this.delegate == other.delegate;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this.delegate) * 31 + this.index;
    }
}

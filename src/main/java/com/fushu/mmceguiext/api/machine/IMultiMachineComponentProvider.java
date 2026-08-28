package com.fushu.mmceguiext.api.machine;

import hellfirepvp.modularmachinery.common.machine.MachineComponent;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * Extension point for tiles that expose multiple MMCE machine components.
 *
 * <p>Implementations should keep {@link #getMachineComponentGroupId()} stable for the lifetime of the tile.
 * If a negative value is returned, Modular Machinery: Community Edition More Extensions falls back to the first provided component's group id.</p>
 */
public interface IMultiMachineComponentProvider {

    @Nonnull
    Collection<MachineComponent<?>> provideMachineComponents();

    /**
     * Stable logical group id for the returned components.
     *
     * <p>Use a non-negative id when the returned components should be merged together in the controller.</p>
     */
    default long getMachineComponentGroupId() {
        return -1L;
    }
}

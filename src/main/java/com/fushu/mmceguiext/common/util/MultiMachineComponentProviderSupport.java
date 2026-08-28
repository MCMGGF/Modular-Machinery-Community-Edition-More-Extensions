package com.fushu.mmceguiext.common.util;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.api.machine.IMultiMachineComponentProvider;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import net.minecraft.tileentity.TileEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public final class MultiMachineComponentProviderSupport {
    private static final Logger LOGGER = LogManager.getLogger(MMCEGuiExt.MODID);

    private MultiMachineComponentProviderSupport() {
    }

    @Nonnull
    public static Collection<MachineComponent<?>> resolveMachineComponents(@Nullable final TileEntity tileEntity) {
        if (tileEntity == null) {
            return Collections.emptyList();
        }
        if (tileEntity instanceof IMultiMachineComponentProvider) {
            try {
                return sanitize(((IMultiMachineComponentProvider) tileEntity).provideMachineComponents());
            } catch (RuntimeException | LinkageError error) {
                LOGGER.warn(
                    "Failed to resolve machine components from {}: {}",
                    tileEntity.getClass().getName(),
                    error.toString()
                );
            }
        }
        return Collections.emptyList();
    }

    public static long resolveStableGroupId(@Nullable final TileEntity tileEntity,
                                            @Nonnull final Collection<MachineComponent<?>> components) {
        if (tileEntity instanceof IMultiMachineComponentProvider) {
            try {
                long groupId = ((IMultiMachineComponentProvider) tileEntity).getMachineComponentGroupId();
                if (groupId >= 0L) {
                    return groupId;
                }
            } catch (RuntimeException | LinkageError error) {
                LOGGER.warn(
                    "Failed to resolve machine component group id from {}: {}",
                    tileEntity.getClass().getName(),
                    error.toString()
                );
            }
        }
        for (MachineComponent<?> component : components) {
            if (component == null) {
                continue;
            }
            long groupId = component.getGroupID();
            if (groupId >= 0L) {
                return groupId;
            }
        }
        return -1L;
    }

    @Nonnull
    private static Collection<MachineComponent<?>> sanitize(@Nullable final Collection<?> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<MachineComponent<?>> out = new ArrayList<MachineComponent<?>>(raw.size());
        for (Object entry : raw) {
            if (entry instanceof MachineComponent) {
                out.add((MachineComponent<?>) entry);
            }
        }
        return out.isEmpty() ? Collections.<MachineComponent<?>>emptyList() : out;
    }
}

package com.fushu.mmceguiext.common.util;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.api.machine.IMultiMachineComponentProvider;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import net.minecraft.tileentity.TileEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
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
            return sanitize(((IMultiMachineComponentProvider) tileEntity).provideMachineComponents());
        }
        Collection<MachineComponent<?>> provided = invokeComponentProvider(tileEntity, "provideMachineComponents");
        if (!provided.isEmpty()) {
            return provided;
        }
        return invokeComponentProvider(tileEntity, "provideComponents");
    }

    public static long resolveStableGroupId(@Nullable final TileEntity tileEntity,
                                            @Nonnull final Collection<MachineComponent<?>> components) {
        if (tileEntity instanceof IMultiMachineComponentProvider) {
            long groupId = ((IMultiMachineComponentProvider) tileEntity).getMachineComponentGroupId();
            if (groupId >= 0L) {
                return groupId;
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
    private static Collection<MachineComponent<?>> invokeComponentProvider(@Nonnull final TileEntity tileEntity,
                                                                           @Nonnull final String methodName) {
        try {
            Method method = tileEntity.getClass().getMethod(methodName);
            Object result = method.invoke(tileEntity);
            if (result instanceof Collection) {
                return sanitize((Collection<?>) result);
            }
            if (result != null) {
                LOGGER.warn(
                    "Ignoring custom machine component provider {}#{} because it returned {} instead of Collection",
                    tileEntity.getClass().getName(),
                    methodName,
                    result.getClass().getName()
                );
            }
        } catch (NoSuchMethodException ignored) {
        } catch (LinkageError error) {
            LOGGER.warn(
                "Failed to inspect custom machine component provider {}#{} because of a linkage error: {}",
                tileEntity.getClass().getName(),
                methodName,
                error.toString()
            );
        } catch (Exception ex) {
            LOGGER.warn(
                "Failed to invoke custom machine component provider {}#{}: {}",
                tileEntity.getClass().getName(),
                methodName,
                ex.toString()
            );
        }
        return Collections.emptyList();
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

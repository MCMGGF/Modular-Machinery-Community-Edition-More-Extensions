package com.fushu.mmceguiext.common.util;

import com.fushu.mmceguiext.api.machine.IMultiMachineComponentProvider;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import net.minecraft.tileentity.TileEntity;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class MultiMachineComponentProviderSupportTest {

    @Test
    public void resolvesComponentsFromInterfaceBeforeReflection() {
        DummyProviderTile tile = new DummyProviderTile();

        Collection<MachineComponent<?>> resolved = MultiMachineComponentProviderSupport.resolveMachineComponents(tile);

        assertEquals(2, resolved.size());
        assertSame(tile.first, resolved.iterator().next());
        assertEquals(777L, MultiMachineComponentProviderSupport.resolveStableGroupId(tile, resolved));
    }

    @Test
    public void fallsBackToProvideMachineComponentsReflection() {
        ReflectiveProviderTile tile = new ReflectiveProviderTile();

        Collection<MachineComponent<?>> resolved = MultiMachineComponentProviderSupport.resolveMachineComponents(tile);

        assertEquals(1, resolved.size());
        assertSame(tile.component, resolved.iterator().next());
        assertEquals(888L, MultiMachineComponentProviderSupport.resolveStableGroupId(tile, resolved));
    }

    @Test
    public void fallsBackToProvideComponentsReflection() {
        LegacyReflectiveProviderTile tile = new LegacyReflectiveProviderTile();

        Collection<MachineComponent<?>> resolved = MultiMachineComponentProviderSupport.resolveMachineComponents(tile);

        assertEquals(1, resolved.size());
        assertSame(tile.component, resolved.iterator().next());
        assertEquals(999L, MultiMachineComponentProviderSupport.resolveStableGroupId(tile, resolved));
    }

    private static MachineComponent<Object> component(final long groupId) {
        return new MachineComponent<Object>(IOType.INPUT) {
            @Override
            public Object getContainerProvider() {
                return new Object();
            }

            @Override
            public ComponentType getComponentType() {
                return ComponentTypesMM.COMPONENT_ITEM;
            }

            @Override
            public long getGroupID() {
                return groupId;
            }
        };
    }

    private static final class DummyProviderTile extends TileEntity implements IMultiMachineComponentProvider {
        private final MachineComponent<Object> first = component(111L);
        private final MachineComponent<Object> second = component(222L);

        @Nonnull
        @Override
        public Collection<MachineComponent<?>> provideMachineComponents() {
            return Arrays.<MachineComponent<?>>asList(this.first, this.second);
        }

        @Override
        public long getMachineComponentGroupId() {
            return 777L;
        }
    }

    @SuppressWarnings("unused")
    private static final class ReflectiveProviderTile extends TileEntity {
        private final MachineComponent<Object> component = component(888L);

        @Nonnull
        public Collection<MachineComponent<?>> provideMachineComponents() {
            return Collections.<MachineComponent<?>>singletonList(this.component);
        }
    }

    @SuppressWarnings("unused")
    private static final class LegacyReflectiveProviderTile extends TileEntity {
        private final MachineComponent<Object> component = component(999L);

        @Nonnull
        public Collection<MachineComponent<?>> provideComponents() {
            return Collections.<MachineComponent<?>>singletonList(this.component);
        }
    }
}

package com.fushu.mmceguiext.common.util;

import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;

/**
 * Stable component identity used to expose controller-backed virtual Smart Interfaces to MMCE.
 *
 * <p>This must remain a top-level class. Mixin relocates nested classes declared inside a mixin,
 * while javac may generate synthetic constructors that still reference the original mixin class.
 */
public final class VirtualSmartInterfaceComponent
    extends MachineComponent<VirtualSmartInterfaceComponent> {

    public VirtualSmartInterfaceComponent() {
        super(IOType.INPUT);
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentTypesMM.COMPONENT_SMART_INTERFACE;
    }

    @Override
    public VirtualSmartInterfaceComponent getContainerProvider() {
        return this;
    }
}

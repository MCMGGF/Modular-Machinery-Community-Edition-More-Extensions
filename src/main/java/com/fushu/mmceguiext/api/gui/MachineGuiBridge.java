package com.fushu.mmceguiext.api.gui;

import com.fushu.mmceguiext.client.gui.GuiFactoryControllerResizable;
import com.fushu.mmceguiext.client.gui.GuiMachineControllerResizable;
import hellfirepvp.modularmachinery.common.container.ContainerController;
import hellfirepvp.modularmachinery.common.container.ContainerFactoryController;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Objects;

/**
 * Stable public bridge for opening MMCE More Extensions resizable controller GUIs programmatically.
 * <p>
 * Use these methods instead of directly constructing GUI classes,
 * so that the implementation can evolve without breaking addon mods.
 */
@SideOnly(Side.CLIENT)
public final class MachineGuiBridge {
    private MachineGuiBridge() {
    }

    /**
     * Creates a resizable machine controller GUI for the given container.
     * Works with both plain {@link ContainerController} instances.
     */
    public static GuiScreen createMachineControllerScreen(ContainerController container) {
        return new GuiMachineControllerResizable(Objects.requireNonNull(container, "container"));
    }

    /**
     * Creates a resizable factory controller GUI for the given container.
     * Works with {@link ContainerFactoryController} instances.
     */
    public static GuiScreen createFactoryControllerScreen(ContainerFactoryController container) {
        return new GuiFactoryControllerResizable(Objects.requireNonNull(container, "container"));
    }
}

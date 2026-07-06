package com.fushu.mmceguiext.api.gui;

import com.fushu.mmceguiext.client.config.MachineGuiStyleManager;
import net.minecraft.util.ResourceLocation;

/**
 * Public bridge for addons that keep GUI configuration outside MMCE machine JSON files.
 */
public final class MachineGuiStyleApi {
    private MachineGuiStyleApi() {
    }

    public static MachineGuiStyleManager.ControllerStyle newControllerStyle() {
        return new MachineGuiStyleManager.ControllerStyle();
    }

    /**
     * Resolves a defensive copy of the current controller style for the machine key.
     */
    public static MachineGuiStyleManager.ControllerStyle resolveMachineControllerStyle(ResourceLocation machineName) {
        return MachineGuiStyleManager.resolveMachineController(machineName);
    }

    /**
     * Resolves a defensive copy of the current factory-controller style for the machine key.
     */
    public static MachineGuiStyleManager.ControllerStyle resolveFactoryControllerStyle(ResourceLocation machineName) {
        return MachineGuiStyleManager.resolveFactoryController(machineName);
    }

    /**
     * Registers a defensive copy of the supplied style.
     */
    public static void registerMachineControllerStyle(ResourceLocation machineName,
                                                      MachineGuiStyleManager.ControllerStyle style) {
        MachineGuiStyleManager.registerExternalMachineControllerStyle(machineName, style);
    }

    /**
     * Registers a defensive copy of the supplied style.
     */
    public static void registerFactoryControllerStyle(ResourceLocation machineName,
                                                      MachineGuiStyleManager.ControllerStyle style) {
        MachineGuiStyleManager.registerExternalFactoryControllerStyle(machineName, style);
    }

    public static void clearExternalStyles() {
        MachineGuiStyleManager.clearExternalStyles();
    }
}

package com.fushu.mmceguiext.api.gui;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Optional controller-side hook for addons that want a GUI style key different
 * from the backing MMCE machine registry name.
 */
public interface IMachineGuiStyleProvider {
    @Nullable
    ResourceLocation getMachineControllerGuiStyle();
}

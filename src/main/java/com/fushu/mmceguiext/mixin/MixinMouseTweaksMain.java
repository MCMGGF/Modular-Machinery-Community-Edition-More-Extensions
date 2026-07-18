package com.fushu.mmceguiext.mixin;

import com.fushu.mmceguiext.client.compat.MouseTweaksCompatibility;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yalter.mousetweaks.Main;

@Mixin(value = Main.class, remap = false)
public abstract class MixinMouseTweaksMain {

    @Inject(
        method = "findHandler(Lnet/minecraft/client/gui/GuiScreen;)Lyalter/mousetweaks/impl/IGuiScreenHandler;",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void mmceguiext$avoidMissingSpecializedHandler(
        GuiScreen currentScreen,
        CallbackInfoReturnable<Object> cir
    ) {
        String missingHandler =
            MouseTweaksCompatibility.findMissingSpecializedHandler(currentScreen);
        if (missingHandler == null) {
            return;
        }
        MouseTweaksCompatibility.warnMissingHandlerOnce(missingHandler, currentScreen);
        cir.setReturnValue(null);
    }
}

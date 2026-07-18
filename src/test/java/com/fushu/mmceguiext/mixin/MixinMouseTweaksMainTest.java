package com.fushu.mmceguiext.mixin;

import net.minecraft.client.gui.GuiScreen;
import org.junit.Test;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MixinMouseTweaksMainTest {
    private static final String TARGET_DESCRIPTOR =
        "findHandler(Lnet/minecraft/client/gui/GuiScreen;)"
            + "Lyalter/mousetweaks/impl/IGuiScreenHandler;";

    @Test
    public void mixinDescriptorMatchesOfficialMouseTweaksTarget() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        Class<?> main = Class.forName("yalter.mousetweaks.Main", false, loader);
        Method target = main.getDeclaredMethod("findHandler", GuiScreen.class);
        assertEquals(
            "yalter.mousetweaks.impl.IGuiScreenHandler",
            target.getReturnType().getName()
        );

        Method callback = MixinMouseTweaksMain.class.getDeclaredMethod(
            "mmceguiext$avoidMissingSpecializedHandler",
            GuiScreen.class,
            CallbackInfoReturnable.class
        );
        Inject inject = callback.getAnnotation(Inject.class);
        assertArrayEquals(new String[] {TARGET_DESCRIPTOR}, inject.method());
        assertTrue(inject.cancellable());

        Mixin mixin = MixinMouseTweaksMain.class.getAnnotation(Mixin.class);
        assertArrayEquals(new String[] {"yalter.mousetweaks.Main"}, mixin.targets());
    }
}

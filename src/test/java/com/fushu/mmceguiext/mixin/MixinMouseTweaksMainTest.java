package com.fushu.mmceguiext.mixin;

import net.minecraft.client.gui.GuiScreen;
import org.junit.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MixinMouseTweaksMainTest {
    private static final String MIXIN_CLASS_RESOURCE =
        "com/fushu/mmceguiext/mixin/MixinMouseTweaksMain.class";
    private static final String MIXIN_ANNOTATION =
        "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String PSEUDO_ANNOTATION =
        "Lorg/spongepowered/asm/mixin/Pseudo;";
    private static final String TARGET_CLASS = "yalter.mousetweaks.Main";
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
    }

    @Test
    public void mixinUsesOptionalStringTargetWithoutLoadingMouseTweaksMain() throws Exception {
        InputStream classBytes = getClass().getClassLoader()
            .getResourceAsStream(MIXIN_CLASS_RESOURCE);
        assertNotNull(classBytes);

        final boolean[] pseudo = {false};
        final boolean[] remap = {true};
        final List<String> targets = new ArrayList<>();
        try {
            new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM5) {
                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    if (PSEUDO_ANNOTATION.equals(descriptor)) {
                        pseudo[0] = true;
                    }
                    if (!MIXIN_ANNOTATION.equals(descriptor)) {
                        return super.visitAnnotation(descriptor, visible);
                    }
                    return new AnnotationVisitor(Opcodes.ASM5) {
                        @Override
                        public void visit(String name, Object value) {
                            if ("remap".equals(name)) {
                                remap[0] = (Boolean) value;
                            }
                        }

                        @Override
                        public AnnotationVisitor visitArray(String name) {
                            if (!"targets".equals(name)) {
                                return super.visitArray(name);
                            }
                            return new AnnotationVisitor(Opcodes.ASM5) {
                                @Override
                                public void visit(String ignored, Object value) {
                                    targets.add((String) value);
                                }
                            };
                        }
                    };
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } finally {
            classBytes.close();
        }

        assertTrue(pseudo[0]);
        assertFalse(remap[0]);
        assertEquals(Collections.singletonList(TARGET_CLASS), targets);
    }
}

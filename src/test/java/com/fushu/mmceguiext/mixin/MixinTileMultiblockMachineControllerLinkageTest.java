package com.fushu.mmceguiext.mixin;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MixinTileMultiblockMachineControllerLinkageTest {
    private static final String MIXIN_INTERNAL_NAME =
        "com/fushu/mmceguiext/mixin/MixinTileMultiblockMachineController";

    @Test
    public void injectedMethodsDoNotConstructRelocatedMixinInnerClasses() throws IOException {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
            MIXIN_INTERNAL_NAME + ".class"
        );
        assertNotNull("Compiled controller mixin class is missing", stream);

        final List<String> unsafeConstructors = new ArrayList<String>();
        try {
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM5) {
                @Override
                public MethodVisitor visitMethod(
                    final int access,
                    final String name,
                    final String descriptor,
                    final String signature,
                    final String[] exceptions
                ) {
                    return new MethodVisitor(Opcodes.ASM5) {
                        @Override
                        public void visitMethodInsn(
                            final int opcode,
                            final String owner,
                            final String methodName,
                            final String methodDescriptor,
                            final boolean isInterface
                        ) {
                            if (opcode == Opcodes.INVOKESPECIAL
                                && "<init>".equals(methodName)
                                && (owner.startsWith(MIXIN_INTERNAL_NAME + "$")
                                    || methodDescriptor.contains(MIXIN_INTERNAL_NAME + "$"))) {
                                unsafeConstructors.add(
                                    name + " -> " + owner + methodDescriptor
                                );
                            }
                        }
                    };
                }
            }, 0);
        } finally {
            stream.close();
        }

        assertTrue(
            "Mixin inner-class constructors are unsafe after target-class relocation: "
                + unsafeConstructors,
            unsafeConstructors.isEmpty()
        );
    }
}

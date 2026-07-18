package com.fushu.mmceguiext.client.compat;

import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MouseTweaksCompatibilityTest {

    @Test
    public void availableOfficialHandlerKeepsNormalMouseTweaksBehavior() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        Object screen = apiProxy(MouseTweaksCompatibility.API_V2, loader);

        assertNull(MouseTweaksCompatibility.findMissingSpecializedHandler(screen, loader));
    }

    @Test
    public void missingMatchingHandlerDisablesOnlyTheAffectedGui() throws Exception {
        ClassLoader parent = getClass().getClassLoader();
        Object screen = apiProxy(MouseTweaksCompatibility.API_V2, parent);
        ClassLoader filtered = new MissingClassLoader(
            parent,
            MouseTweaksCompatibility.HANDLER_V2
        );

        assertEquals(
            MouseTweaksCompatibility.HANDLER_V2,
            MouseTweaksCompatibility.findMissingSpecializedHandler(screen, filtered)
        );
        assertNull(
            MouseTweaksCompatibility.findMissingSpecializedHandler(new Object(), filtered)
        );
    }

    private static Object apiProxy(String apiClassName, ClassLoader loader) throws Exception {
        Class<?> apiType = Class.forName(apiClassName, false, loader);
        return Proxy.newProxyInstance(
            apiType.getClassLoader(),
            new Class<?>[] {apiType},
            (proxy, method, args) -> {
                if (method.getReturnType() == Boolean.TYPE) {
                    return Boolean.FALSE;
                }
                if (method.getReturnType() == Integer.TYPE) {
                    return Integer.valueOf(0);
                }
                return null;
            }
        );
    }

    private static final class MissingClassLoader extends ClassLoader {
        private final String missingClass;

        private MissingClassLoader(ClassLoader parent, String missingClass) {
            super(parent);
            this.missingClass = missingClass;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (missingClass.equals(name)) {
                throw new ClassNotFoundException(name);
            }
            return super.loadClass(name, resolve);
        }
    }
}

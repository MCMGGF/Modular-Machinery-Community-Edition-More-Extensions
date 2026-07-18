package com.fushu.mmceguiext.client.compat;

import com.fushu.mmceguiext.MMCEGuiExt;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class MouseTweaksCompatibility {
    static final String API_EX = "yalter.mousetweaks.api.IMTModGuiContainer2Ex";
    static final String HANDLER_EX = "yalter.mousetweaks.handlers.IMTModGuiContainer2ExHandler";
    static final String API_V2 = "yalter.mousetweaks.api.IMTModGuiContainer2";
    static final String HANDLER_V2 = "yalter.mousetweaks.handlers.IMTModGuiContainer2Handler";
    static final String API_LEGACY = "yalter.mousetweaks.api.IMTModGuiContainer";
    static final String HANDLER_LEGACY = "yalter.mousetweaks.handlers.IMTModGuiContainerHandler";

    private static final String[][] SPECIALIZED_HANDLERS = {
        {API_EX, HANDLER_EX},
        {API_V2, HANDLER_V2},
        {API_LEGACY, HANDLER_LEGACY}
    };
    private static final Set<String> WARNED_HANDLERS =
        Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private MouseTweaksCompatibility() {
    }

    @Nullable
    public static String findMissingSpecializedHandler(@Nullable Object screen) {
        if (screen == null) {
            return null;
        }
        ClassLoader loader = screen.getClass().getClassLoader();
        if (loader == null) {
            loader = MouseTweaksCompatibility.class.getClassLoader();
        }
        return findMissingSpecializedHandler(screen, loader);
    }

    @Nullable
    static String findMissingSpecializedHandler(Object screen, @Nullable ClassLoader loader) {
        if (screen == null || loader == null) {
            return null;
        }

        for (String[] pair : SPECIALIZED_HANDLERS) {
            Class<?> apiType = tryLoad(pair[0], loader);
            if (apiType == null || !apiType.isInstance(screen)) {
                continue;
            }
            return tryLoad(pair[1], loader) == null ? pair[1] : null;
        }
        return null;
    }

    public static void warnMissingHandlerOnce(String handlerClass, Object screen) {
        if (handlerClass == null || screen == null || !WARNED_HANDLERS.add(handlerClass)) {
            return;
        }
        MMCEGuiExt.logger().warn(
            "Mouse Tweaks handler '{}' is unavailable for GUI '{}'; Mouse Tweaks will be disabled for this GUI instead of crashing.",
            handlerClass,
            screen.getClass().getName()
        );
    }

    @Nullable
    private static Class<?> tryLoad(String className, ClassLoader loader) {
        try {
            return Class.forName(className, false, loader);
        } catch (ClassNotFoundException | LinkageError ignored) {
            return null;
        }
    }
}

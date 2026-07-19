package com.fushu.mmceguiext.client.compat;

import com.fushu.mmceguiext.MMCEGuiExt;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class MouseTweaksClassLoaderGuard {
    static final List<String> PRELOAD_CLASSES = Collections.unmodifiableList(Arrays.asList(
        "yalter.mousetweaks.api.IMTModGuiContainer",
        "yalter.mousetweaks.api.IMTModGuiContainer2",
        "yalter.mousetweaks.api.IMTModGuiContainer2Ex",
        "yalter.mousetweaks.impl.IGuiScreenHandler",
        "yalter.mousetweaks.impl.IMouseState",
        "yalter.mousetweaks.impl.MouseButton",
        "yalter.mousetweaks.handlers.GuiContainerHandler",
        "yalter.mousetweaks.handlers.GuiContainerCreativeHandler",
        "yalter.mousetweaks.handlers.IMTModGuiContainerHandler",
        "yalter.mousetweaks.handlers.IMTModGuiContainer2Handler",
        "yalter.mousetweaks.handlers.IMTModGuiContainer2ExHandler",
        "yalter.mousetweaks.reflect.ReflectionCache",
        "yalter.mousetweaks.reflect.Reflection",
        "yalter.mousetweaks.SimpleMouseState",
        "yalter.mousetweaks.MouseState",
        "yalter.mousetweaks.Main"
    ));

    private static boolean initialized;

    private MouseTweaksClassLoaderGuard() {
    }

    public static synchronized void initialize() {
        if (initialized || !Loader.isModLoaded("mousetweaks")) {
            return;
        }
        initialized = true;

        ClassLoader loader = MouseTweaksClassLoaderGuard.class.getClassLoader();
        List<String> failures = preloadClasses(loader, PRELOAD_CLASSES);
        if (failures.isEmpty()) {
            MMCEGuiExt.logger().info(
                "Mouse Tweaks core classes were preloaded without transforming its Main class."
            );
            return;
        }

        MMCEGuiExt.logger().warn(
            "Mouse Tweaks class-loader recovery could not load {}. "
                + "Mouse Tweaks may need to be removed on this Cleanroom installation.",
            failures
        );
    }

    static List<String> preloadClasses(
        @Nullable ClassLoader loader,
        Collection<String> classNames
    ) {
        if (loader == null || classNames == null || classNames.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> targets = new LinkedHashSet<String>();
        for (String className : classNames) {
            if (className != null && !className.trim().isEmpty()) {
                targets.add(className.trim());
            }
        }
        if (targets.isEmpty()) {
            return Collections.emptyList();
        }

        clearFailedClassEntries(loader, targets);
        List<String> failures = new ArrayList<String>();
        for (String className : targets) {
            try {
                Class.forName(className, false, loader);
            } catch (ClassNotFoundException | LinkageError e) {
                failures.add(className);
            }
        }
        return failures;
    }

    static void clearFailedClassEntries(
        @Nullable ClassLoader loader,
        Collection<String> classNames
    ) {
        if (loader == null || classNames == null || classNames.isEmpty()) {
            return;
        }

        Set<String> names = new LinkedHashSet<String>(classNames);
        try {
            Method getInvalidClasses = findMethod(loader.getClass(), "getInvalidClasses");
            if (getInvalidClasses != null) {
                Object invalid = getInvalidClasses.invoke(loader);
                if (invalid instanceof Set<?>) {
                    ((Set<?>) invalid).removeAll(names);
                }
            }

            Method clearNegativeEntries =
                findMethod(loader.getClass(), "clearNegativeEntries", Set.class);
            if (clearNegativeEntries != null) {
                clearNegativeEntries.invoke(loader, names);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Standard LaunchClassLoader has no Cleanroom negative-cache API.
        }
    }

    @Nullable
    private static Method findMethod(
        @Nullable Class<?> type,
        String name,
        Class<?>... parameterTypes
    ) {
        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}

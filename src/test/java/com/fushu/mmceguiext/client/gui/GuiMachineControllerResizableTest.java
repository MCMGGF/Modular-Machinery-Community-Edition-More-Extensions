package com.fushu.mmceguiext.client.gui;

import com.fushu.mmceguiext.MMCEGuiExtConfig;
import com.fushu.mmceguiext.client.config.MachineGuiStyleManager;
import sun.misc.Unsafe;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GuiMachineControllerResizableTest {
    @Test
    public void topmostSliderPrefersForegroundThenPriorityThenLatestEntry() throws Exception {
        GuiMachineControllerResizable gui = allocateGui();
        Object background = newSlider("background", false, 100, 10, 10, 80, 12);
        Object foregroundLow = newSlider("foregroundLow", true, 0, 10, 10, 80, 12);
        Object foregroundHigh = newSlider("foregroundHigh", true, 7, 10, 10, 80, 12);
        Object foregroundLater = newSlider("foregroundLater", true, 7, 10, 10, 80, 12);

        List<Object> sliders = new ArrayList<Object>();
        sliders.add(background);
        sliders.add(foregroundLow);
        sliders.add(foregroundHigh);
        sliders.add(foregroundLater);
        set(gui, "customSliders", sliders);
        set(gui, "activePageId", "main");

        Object hit = invoke(gui, "findTopmostSliderAt", new Class<?>[] {int.class, int.class}, Integer.valueOf(20), Integer.valueOf(15));
        assertSame(foregroundLater, hit);
    }

    @Test
    public void modalSliderReleaseClearsDraggingStateOnRuntimeSnapshot() throws Exception {
        GuiMachineControllerResizable gui = allocateGui();
        Object dragged = newSlider("dragged", true, 5, 10, 10, 80, 12);
        Object runtime = newRuntimeState();
        set(runtime, "draggingSlider", dragged);
        set(gui, "modalSubGuiStack", new ArrayList<Object>(Collections.singletonList(runtime)));
        set(gui, "currentRuntimeState", newRuntimeState());
        set(gui, "draggingSlider", dragged);

        invoke(gui, "handleTopModalMouseReleased", new Class<?>[] {int.class, int.class, int.class}, Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0));

        assertNull(get(runtime, "draggingSlider"));
        assertNull(get(gui, "draggingSlider"));
    }

    @Test
    public void sliderThumbOutsideTrackCanStartDragging() throws Exception {
        GuiMachineControllerResizable gui = allocateGui();
        Object slider = newSlider("thumb", true, 5, 10, 10, 80, 12);
        set(slider, "thumbHeight", Integer.valueOf(14));
        set(gui, "customSliders", new ArrayList<Object>(Collections.singletonList(slider)));
        set(gui, "activePageId", "main");
        set(gui, "guiLeft", Integer.valueOf(100));
        set(gui, "guiTop", Integer.valueOf(50));

        Object started = invoke(
            gui,
            "startSliderDragAt",
            new Class<?>[] {int.class, int.class},
            Integer.valueOf(147),
            Integer.valueOf(73)
        );

        assertSame(slider, started);
        assertSame(slider, get(gui, "draggingSlider"));
    }

    @Test
    public void activeSliderDragIsHandledBeforeBackgroundRouting() throws Exception {
        GuiMachineControllerResizable gui = allocateGui();
        Object slider = newSlider("dragged", true, 5, 10, 10, 80, 12);
        set(gui, "draggingSlider", slider);
        set(gui, "guiLeft", Integer.valueOf(0));
        set(gui, "guiTop", Integer.valueOf(0));

        assertEquals(Boolean.TRUE, invoke(
            gui,
            "handleActiveSliderMouseDrag",
            new Class<?>[] {int.class, int.class, int.class},
            Integer.valueOf(50),
            Integer.valueOf(16),
            Integer.valueOf(0)
        ));
        assertEquals(Boolean.FALSE, invoke(
            gui,
            "handleActiveSliderMouseDrag",
            new Class<?>[] {int.class, int.class, int.class},
            Integer.valueOf(50),
            Integer.valueOf(16),
            Integer.valueOf(1)
        ));
    }

    @Test
    public void hotkeyMatcherAcceptsNamedKeysAndExactModifiers() throws Exception {
        assertEquals(Boolean.TRUE, invokeStatic(
            "matchesHotkey",
            new Class<?>[] {String.class, char.class, int.class, boolean.class, boolean.class, boolean.class},
            "C",
            Character.valueOf('c'),
            Integer.valueOf(0),
            Boolean.FALSE,
            Boolean.FALSE,
            Boolean.FALSE
        ));
        assertEquals(Boolean.TRUE, invokeStatic(
            "matchesHotkey",
            new Class<?>[] {String.class, char.class, int.class, boolean.class, boolean.class, boolean.class},
            "ctrl+KEY_C",
            Character.valueOf('c'),
            Integer.valueOf(0),
            Boolean.FALSE,
            Boolean.TRUE,
            Boolean.FALSE
        ));
        assertEquals(Boolean.FALSE, invokeStatic(
            "matchesHotkey",
            new Class<?>[] {String.class, char.class, int.class, boolean.class, boolean.class, boolean.class},
            "ctrl+KEY_C",
            Character.valueOf('c'),
            Integer.valueOf(0),
            Boolean.FALSE,
            Boolean.FALSE,
            Boolean.FALSE
        ));
    }

    @Test
    public void hiddenHotkeyButtonTriggersPageAction() throws Exception {
        GuiMachineControllerResizable gui = allocateGui();
        Object hotkeyButton = newHotkeyPageButton("C", "settings", true);
        set(gui, "customButtons", new ArrayList<Object>(Collections.singletonList(hotkeyButton)));
        set(gui, "activePageId", "main");

        assertEquals(Boolean.TRUE, invoke(
            gui,
            "handleCustomButtonKeyTyped",
            new Class<?>[] {char.class, int.class},
            Character.valueOf('c'),
            Integer.valueOf(0)
        ));
        assertEquals("settings", get(gui, "activePageId"));
    }

    @Test
    public void hotkeyButtonCanActivateWithoutConsumingKey() throws Exception {
        GuiMachineControllerResizable gui = allocateGui();
        Object hotkeyButton = newHotkeyPageButton("C", "settings", false);
        set(gui, "customButtons", new ArrayList<Object>(Collections.singletonList(hotkeyButton)));
        set(gui, "activePageId", "main");

        assertEquals(Boolean.FALSE, invoke(
            gui,
            "handleCustomButtonKeyTyped",
            new Class<?>[] {char.class, int.class},
            Character.valueOf('c'),
            Integer.valueOf(0)
        ));
        assertEquals("settings", get(gui, "activePageId"));
    }

    @Test
    public void setWidthHeightKeepsOversizedGuiWhenOffscreenIsAllowed() throws Exception {
        GuiMachineControllerResizable gui = allocateGui();
        MMCEGuiExtConfig.MachineController original = MMCEGuiExtConfig.machineController;
        MMCEGuiExtConfig.MachineController cfg = new MMCEGuiExtConfig.MachineController();
        cfg.guiWidth = 900;
        cfg.guiHeight = 500;
        cfg.allowOffscreenGui = true;

        try {
            MMCEGuiExtConfig.machineController = cfg;
            set(gui, "width", Integer.valueOf(400));
            set(gui, "height", Integer.valueOf(240));
            set(gui, "styleOverride", MachineGuiStyleManager.ControllerStyle.EMPTY);
            invoke(gui, "setWidthHeight", new Class<?>[0]);

            assertEquals(Integer.valueOf(900), get(gui, "renderWidth"));
            assertEquals(Integer.valueOf(500), get(gui, "renderHeight"));
        } finally {
            MMCEGuiExtConfig.machineController = original;
        }
    }

    @Test
    public void legacyConfigSmartInterfaceFallbackDoesNotEnableDefaultEditor() throws Exception {
        GuiMachineControllerResizable gui = allocateGui();
        MMCEGuiExtConfig.MachineController cfg = new MMCEGuiExtConfig.MachineController();
        cfg.enableSmartInterfaceEditor = true;
        cfg.smartInterfaceEditorVirtualKey = "mmcege_virtual_port";

        set(gui, "styleOverride", MachineGuiStyleManager.ControllerStyle.EMPTY);

        assertFalse(((Boolean) invoke(
            gui,
            "getSmartInterfaceEditorEnabled",
            new Class<?>[] {MMCEGuiExtConfig.MachineController.class},
            cfg
        )).booleanValue());
    }

    @Test
    public void styleSmartInterfaceFallbackStillHonorsExplicitJsonKey() throws Exception {
        GuiMachineControllerResizable gui = allocateGui();
        MMCEGuiExtConfig.MachineController cfg = new MMCEGuiExtConfig.MachineController();
        cfg.enableSmartInterfaceEditor = false;
        cfg.smartInterfaceEditorVirtualKey = "mmcege_virtual_port";
        MachineGuiStyleManager.ControllerStyle style = new MachineGuiStyleManager.ControllerStyle();
        style.smartInterfaceEditorVirtualKey = "demo_default_port_a,demo_default_port_b";

        set(gui, "styleOverride", style);

        @SuppressWarnings("unchecked")
        List<String> keys = (List<String>) invoke(
            gui,
            "getSmartInterfaceEditorVirtualKeys",
            new Class<?>[] {MMCEGuiExtConfig.MachineController.class},
            cfg
        );

        assertEquals(2, keys.size());
        assertEquals("demo_default_port_a", keys.get(0));
        assertEquals("demo_default_port_b", keys.get(1));
    }

    @Test
    public void pressedTextureOnlyButtonCreatesVisibleWidget() throws Exception {
        GuiMachineControllerResizable gui = allocateGui();
        MachineGuiStyleManager.ControllerStyle style = new MachineGuiStyleManager.ControllerStyle();
        MachineGuiStyleManager.ButtonStyle buttonStyle = pressedTextureOnlyButtonStyle();
        style.buttons = Collections.singletonList(buttonStyle);
        set(gui, "styleOverride", style);
        set(gui, "renderWidth", Integer.valueOf(176));
        set(gui, "renderHeight", Integer.valueOf(166));

        invoke(
            gui,
            "initCustomButtons",
            new Class<?>[] {MMCEGuiExtConfig.MachineController.class},
            new MMCEGuiExtConfig.MachineController()
        );

        List<?> buttons = (List<?>) get(gui, "customButtons");
        assertEquals(1, buttons.size());
        assertNotNull(get(buttons.get(0), "button"));
        assertTrue(get(buttons.get(0), "button") instanceof GuiTexturedButton);
    }

    private static GuiMachineControllerResizable allocateGui() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        GuiMachineControllerResizable gui = (GuiMachineControllerResizable) unsafe.allocateInstance(GuiMachineControllerResizable.class);
        set(gui, "customSliders", new ArrayList<Object>());
        set(gui, "customButtons", new ArrayList<Object>());
        set(gui, "customSmartEditors", new ArrayList<Object>());
        set(gui, "backgroundTextureLayers", new ArrayList<Object>());
        set(gui, "foregroundTextureLayers", new ArrayList<Object>());
        set(gui, "layerRuntimeStates", new java.util.HashMap<Object, Object>());
        set(gui, "textureLayerIds", new java.util.HashSet<Object>());
        set(gui, "panelScroll", new java.util.HashMap<Object, Object>());
        set(gui, "panelMaxScroll", new java.util.HashMap<Object, Object>());
        set(gui, "smartInterfaceVirtualInputCache", new java.util.HashMap<Object, Object>());
        set(gui, "modalSubGuiStack", new ArrayList<Object>());
        set(gui, "replaceSubGuiStack", new ArrayList<Object>());
        return gui;
    }

    private static Object newSlider(String id, boolean foreground, int priority, int x, int y, int width, int height) throws Exception {
        Class<?> sliderClass = Class.forName("com.fushu.mmceguiext.client.gui.GuiMachineControllerResizable$CustomSlider");
        Constructor<?> ctor = sliderClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object slider = ctor.newInstance();
        set(slider, "id", id);
        set(slider, "key", id + "_key");
        set(slider, "foreground", Boolean.valueOf(foreground));
        set(slider, "priority", Integer.valueOf(priority));
        set(slider, "x", Integer.valueOf(x));
        set(slider, "y", Integer.valueOf(y));
        set(slider, "width", Integer.valueOf(width));
        set(slider, "height", Integer.valueOf(height));
        set(slider, "min", Float.valueOf(0.0F));
        set(slider, "max", Float.valueOf(10.0F));
        set(slider, "step", Float.valueOf(0.0F));
        set(slider, "value", Float.valueOf(5.0F));
        set(slider, "thumbWidth", Integer.valueOf(8));
        set(slider, "thumbHeight", Integer.valueOf(height));
        set(slider, "visible", Boolean.TRUE);
        set(slider, "page", "main");
        return slider;
    }

    private static Object newHotkeyPageButton(String hotkey, String targetPage, boolean consumeHotkey) throws Exception {
        Class<?> buttonClass = Class.forName("com.fushu.mmceguiext.client.gui.GuiMachineControllerResizable$CustomButton");
        Constructor<?> ctor = buttonClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object button = ctor.newInstance();
        set(button, "action", "page");
        set(button, "targetPage", targetPage);
        set(button, "page", null);
        set(button, "hotkeys", new ArrayList<String>(Collections.singletonList(hotkey)));
        set(button, "consumeHotkey", Boolean.valueOf(consumeHotkey));
        return button;
    }

    private static MachineGuiStyleManager.ButtonStyle pressedTextureOnlyButtonStyle() {
        MachineGuiStyleManager.ButtonStyle style = new MachineGuiStyleManager.ButtonStyle();
        style.id = "pressed_only";
        style.action = "page";
        style.targetPage = "main";
        style.width = Integer.valueOf(16);
        style.height = Integer.valueOf(16);
        style.pressedTexture = "mmceguiext:textures/gui/pressed_only.png";
        return style;
    }

    private static Object newRuntimeState() throws Exception {
        Class<?> stateClass = Class.forName("com.fushu.mmceguiext.client.gui.GuiMachineControllerResizable$RuntimeGuiState");
        Constructor<?> ctor = stateClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Object invokeStatic(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = GuiMachineControllerResizable.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    private static void set(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object get(Object target, String fieldName) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}

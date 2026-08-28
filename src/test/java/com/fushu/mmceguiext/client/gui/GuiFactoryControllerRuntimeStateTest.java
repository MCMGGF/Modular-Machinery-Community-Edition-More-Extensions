package com.fushu.mmceguiext.client.gui;

import com.fushu.mmceguiext.MMCEGuiExtConfig;
import com.fushu.mmceguiext.api.gui.IMachineGuiStyleProvider;
import com.fushu.mmceguiext.client.config.MachineGuiStyleManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import org.junit.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GuiFactoryControllerRuntimeStateTest {
    @Test
    public void runtimeStateRestoresParentCollectionsWithoutSubGuiPollution() throws Exception {
        GuiFactoryControllerResizable gui = allocateGui();
        List<Object> parentButtons = new ArrayList<Object>();
        parentButtons.add(new Object());
        Map<String, String> parentVirtualCache = new HashMap<String, String>();
        parentVirtualCache.put("status", "ready");
        Map<String, Integer> parentScroll = new HashMap<String, Integer>();
        parentScroll.put("main", Integer.valueOf(2));

        set(gui, "customButtons", parentButtons);
        set(gui, "customSmartEditors", new ArrayList<Object>());
        set(gui, "customSliders", new ArrayList<Object>());
        set(gui, "backgroundTextureLayers", new ArrayList<Object>());
        set(gui, "foregroundTextureLayers", new ArrayList<Object>());
        set(gui, "layerRuntimeStates", new HashMap<Object, Object>());
        set(gui, "textureLayerIds", new java.util.HashSet<Object>());
        set(gui, "subGuiStyleIndex", new HashMap<Object, Object>());
        set(gui, "styleOverride", MachineGuiStyleManager.ControllerStyle.EMPTY);
        set(gui, "smartInterfaceVirtualInputCache", parentVirtualCache);
        set(gui, "panelScroll", parentScroll);
        set(gui, "panelMaxScroll", new HashMap<String, Integer>());
        set(gui, "activePageId", "main");

        Object parentState = invoke(gui, "captureCurrentRuntimeState");

        parentButtons.clear();
        parentButtons.add(new Object());
        parentButtons.add(new Object());
        parentVirtualCache.put("status", "subgui");
        parentScroll.put("main", Integer.valueOf(99));
        set(gui, "activePageId", "settings");

        invoke(gui, "applyRuntimeState", parentState);

        List<?> restoredButtons = (List<?>) get(gui, "customButtons");
        Map<?, ?> restoredVirtualCache = (Map<?, ?>) get(gui, "smartInterfaceVirtualInputCache");
        Map<?, ?> restoredScroll = (Map<?, ?>) get(gui, "panelScroll");

        assertEquals(1, restoredButtons.size());
        assertEquals("ready", restoredVirtualCache.get("status"));
        assertEquals(Integer.valueOf(2), restoredScroll.get("main"));
        assertEquals("main", get(gui, "activePageId"));
        assertNotSame(parentButtons, restoredButtons);
        assertNotSame(parentVirtualCache, restoredVirtualCache);
        assertNotSame(parentScroll, restoredScroll);
    }

    @Test
    public void runtimeStateRestoresModalDragState() throws Exception {
        GuiFactoryControllerResizable gui = allocateGuiWithRuntimeDefaults();
        set(gui, "modalSubGuiDraggable", Boolean.TRUE);
        set(gui, "modalSubGuiDragHandle", Boolean.TRUE);
        set(gui, "modalSubGuiDragX", Integer.valueOf(4));
        set(gui, "modalSubGuiDragY", Integer.valueOf(5));
        set(gui, "modalSubGuiDragWidth", Integer.valueOf(120));
        set(gui, "modalSubGuiDragHeight", Integer.valueOf(14));
        set(gui, "draggingModalSubGui", Boolean.TRUE);
        set(gui, "modalSubGuiDragOffsetX", Integer.valueOf(33));
        set(gui, "modalSubGuiDragOffsetY", Integer.valueOf(44));

        Object dragState = invoke(gui, "captureCurrentRuntimeState");

        set(gui, "modalSubGuiDraggable", Boolean.FALSE);
        set(gui, "modalSubGuiDragHandle", Boolean.FALSE);
        set(gui, "modalSubGuiDragX", Integer.valueOf(0));
        set(gui, "modalSubGuiDragY", Integer.valueOf(0));
        set(gui, "modalSubGuiDragWidth", Integer.valueOf(0));
        set(gui, "modalSubGuiDragHeight", Integer.valueOf(0));
        set(gui, "draggingModalSubGui", Boolean.FALSE);
        set(gui, "modalSubGuiDragOffsetX", Integer.valueOf(0));
        set(gui, "modalSubGuiDragOffsetY", Integer.valueOf(0));

        invoke(gui, "applyRuntimeState", dragState);

        assertEquals(Boolean.TRUE, get(gui, "modalSubGuiDraggable"));
        assertEquals(Boolean.TRUE, get(gui, "modalSubGuiDragHandle"));
        assertEquals(Integer.valueOf(4), get(gui, "modalSubGuiDragX"));
        assertEquals(Integer.valueOf(5), get(gui, "modalSubGuiDragY"));
        assertEquals(Integer.valueOf(120), get(gui, "modalSubGuiDragWidth"));
        assertEquals(Integer.valueOf(14), get(gui, "modalSubGuiDragHeight"));
        assertEquals(Boolean.TRUE, get(gui, "draggingModalSubGui"));
        assertEquals(Integer.valueOf(33), get(gui, "modalSubGuiDragOffsetX"));
        assertEquals(Integer.valueOf(44), get(gui, "modalSubGuiDragOffsetY"));
    }

    @Test
    public void modalDragHandleHitTestUsesTopHandleUnlessWholeWindowIsRequested() throws Exception {
        GuiFactoryControllerResizable gui = allocateGuiWithRuntimeDefaults();
        set(gui, "activeSubGui", newActiveSubGui("modal"));
        set(gui, "guiLeft", Integer.valueOf(10));
        set(gui, "guiTop", Integer.valueOf(20));
        set(gui, "renderWidth", Integer.valueOf(120));
        set(gui, "renderHeight", Integer.valueOf(80));
        set(gui, "modalSubGuiDragHandle", Boolean.TRUE);
        set(gui, "modalSubGuiDragX", Integer.valueOf(0));
        set(gui, "modalSubGuiDragY", Integer.valueOf(0));
        set(gui, "modalSubGuiDragWidth", Integer.valueOf(0));
        set(gui, "modalSubGuiDragHeight", Integer.valueOf(0));

        assertEquals(Boolean.TRUE, invoke(gui, "isMouseInModalSubGuiDragHandle", new Class<?>[] {int.class, int.class}, Integer.valueOf(15), Integer.valueOf(25)));
        assertEquals(Boolean.FALSE, invoke(gui, "isMouseInModalSubGuiDragHandle", new Class<?>[] {int.class, int.class}, Integer.valueOf(15), Integer.valueOf(45)));

        set(gui, "modalSubGuiDragHandle", Boolean.FALSE);
        assertEquals(Boolean.TRUE, invoke(gui, "isMouseInModalSubGuiDragHandle", new Class<?>[] {int.class, int.class}, Integer.valueOf(15), Integer.valueOf(45)));

        set(gui, "activeSubGui", newActiveSubGui("replace"));
        assertEquals(Boolean.FALSE, invoke(gui, "isMouseInModalSubGuiDragHandle", new Class<?>[] {int.class, int.class}, Integer.valueOf(15), Integer.valueOf(25)));
    }

    @Test
    public void modalSubGuiHotkeyHandlesCustomButtonsWithoutFocusedEditor() throws Exception {
        GuiFactoryControllerResizable gui = allocateGuiWithRuntimeDefaults();
        Object activeSubGui = newActiveSubGui("modal");
        Object runtimeState = get(activeSubGui, "runtimeState");
        Object hotkeyButton = newHotkeyPageButton("C", "settings");
        set(runtimeState, "customButtons", new ArrayList<Object>(Collections.singletonList(hotkeyButton)));
        set(runtimeState, "activePageId", "main");
        set(gui, "activeSubGui", activeSubGui);
        set(gui, "activePageId", "parent");

        assertEquals(Boolean.TRUE, invoke(
            gui,
            "handleModalSubGuiKeyTyped",
            new Class<?>[] {char.class, int.class},
            Character.valueOf('c'),
            Integer.valueOf(0)
        ));

        Object updatedRuntimeState = get(get(gui, "activeSubGui"), "runtimeState");
        assertEquals("settings", get(updatedRuntimeState, "activePageId"));
        assertEquals("parent", get(gui, "activePageId"));
    }

    @Test
    public void movingCurrentGuiAlsoMovesInteractiveControls() throws Exception {
        GuiFactoryControllerResizable gui = allocateGuiWithRuntimeDefaults();
        GuiButton defaultPrev = new GuiButton(1, 12, 22, 10, 10, "<");
        GuiButton customButton = new GuiButton(2, 30, 40, 20, 10, "Run");
        Object custom = newCustomButton(customButton);
        List<Object> customButtons = new ArrayList<Object>();
        customButtons.add(custom);

        set(gui, "guiLeft", Integer.valueOf(10));
        set(gui, "guiTop", Integer.valueOf(20));
        set(gui, "smartInterfacePrevButton", defaultPrev);
        set(gui, "customButtons", customButtons);

        invoke(gui, "moveCurrentGuiTo", new Class<?>[] {int.class, int.class}, Integer.valueOf(25), Integer.valueOf(27));

        assertEquals(Integer.valueOf(25), get(gui, "guiLeft"));
        assertEquals(Integer.valueOf(27), get(gui, "guiTop"));
        assertEquals(27, defaultPrev.x);
        assertEquals(29, defaultPrev.y);
        assertEquals(45, customButton.x);
        assertEquals(47, customButton.y);
    }

    @Test
    public void factorySliderThumbOutsideTrackCanStartDragging() throws Exception {
        GuiFactoryControllerResizable gui = allocateGuiWithRuntimeDefaults();
        Object slider = newSlider("thumb", 10, 10, 80, 12);
        set(slider, "thumbHeight", Integer.valueOf(14));
        set(gui, "customSliders", new ArrayList<Object>(Collections.singletonList(slider)));
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
    public void factoryThreadScrollbarAutomaticHeightUsesVisibleQueueHeight() throws Exception {
        GuiFactoryControllerResizable gui = allocateGuiWithRuntimeDefaults();
        MachineGuiStyleManager.ControllerStyle style = new MachineGuiStyleManager.ControllerStyle();
        style.threadScrollbar = new MachineGuiStyleManager.ThreadScrollbarStyle();
        style.threadScrollbar.height = Integer.valueOf(-1);
        set(gui, "styleOverride", style);

        assertEquals(
            Integer.valueOf(197),
            invoke(
                gui,
                "getThreadScrollbarHeight",
                new Class<?>[] {int.class},
                Integer.valueOf(197)
            )
        );

        style.threadScrollbar.height = Integer.valueOf(123);
        assertEquals(
            Integer.valueOf(123),
            invoke(
                gui,
                "getThreadScrollbarHeight",
                new Class<?>[] {int.class},
                Integer.valueOf(197)
            )
        );
    }

    @Test
    public void factoryActiveSliderDragIsHandledBeforeBackgroundRouting() throws Exception {
        GuiFactoryControllerResizable gui = allocateGuiWithRuntimeDefaults();
        Object slider = newSlider("dragged", 10, 10, 80, 12);
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
    public void modalSliderReleaseOutsideBoundsClearsRuntimeDrag() throws Exception {
        GuiFactoryControllerResizable gui = allocateGuiWithRuntimeDefaults();
        Object activeSubGui = newActiveSubGui("modal");
        Object runtimeState = get(activeSubGui, "runtimeState");
        Object slider = newSlider("modal", 10, 10, 80, 12);
        set(runtimeState, "draggingSlider", slider);
        set(runtimeState, "renderWidth", Integer.valueOf(120));
        set(runtimeState, "renderHeight", Integer.valueOf(80));
        set(gui, "activeSubGui", activeSubGui);

        assertTrue(((Boolean) invoke(
            gui,
            "handleModalSubGuiMouseReleased",
            new Class<?>[] {int.class, int.class, int.class},
            Integer.valueOf(999),
            Integer.valueOf(999),
            Integer.valueOf(0)
        )).booleanValue());
        assertNull(get(get(get(gui, "activeSubGui"), "runtimeState"), "draggingSlider"));
    }

    @Test
    public void legacyConfigSmartInterfaceFallbackDoesNotEnableDefaultEditor() throws Exception {
        GuiFactoryControllerResizable gui = allocateGuiWithRuntimeDefaults();
        MMCEGuiExtConfig.FactoryController cfg = new MMCEGuiExtConfig.FactoryController();
        cfg.enableSmartInterfaceEditor = true;
        cfg.smartInterfaceEditorVirtualKey = "demo_default_port_a,demo_default_port_b";

        assertFalse(((Boolean) invoke(
            gui,
            "getSmartInterfaceEditorEnabled",
            new Class<?>[] {MMCEGuiExtConfig.FactoryController.class},
            cfg
        )).booleanValue());
    }

    @Test
    public void factoryControllerUsesProvidedExternalStyleKey() throws Exception {
        ResourceLocation styleKey = new ResourceLocation("mmceoneblock", "factory_style");
        MachineGuiStyleManager.ControllerStyle style = new MachineGuiStyleManager.ControllerStyle();
        style.texts = new ArrayList<MachineGuiStyleManager.TextStyle>();
        MachineGuiStyleManager.TextStyle text = new MachineGuiStyleManager.TextStyle();
        text.value = "factory-provider-style";
        style.texts.add(text);

        MachineGuiStyleManager.clearExternalStyles();
        try {
            MachineGuiStyleManager.registerExternalFactoryControllerStyle(styleKey, style);
            MachineGuiStyleManager.ControllerStyle resolved =
                (MachineGuiStyleManager.ControllerStyle) invokeStatic(
                    "mergeProvidedFactoryStyle",
                    new Class<?>[] {
                        MachineGuiStyleManager.ControllerStyle.class,
                        IMachineGuiStyleProvider.class
                    },
                    MachineGuiStyleManager.ControllerStyle.EMPTY,
                    new StyleProvider(styleKey)
                );

            assertEquals(1, resolved.texts.size());
            assertEquals("factory-provider-style", resolved.texts.get(0).value);
        } finally {
            MachineGuiStyleManager.clearExternalStyles();
        }
    }

    @Test
    public void factoryPressedTextureOnlyButtonCreatesVisibleWidget() throws Exception {
        GuiFactoryControllerResizable gui = allocateGuiWithRuntimeDefaults();
        MachineGuiStyleManager.ControllerStyle style = new MachineGuiStyleManager.ControllerStyle();
        style.buttons = Collections.singletonList(pressedTextureOnlyButtonStyle());
        set(gui, "styleOverride", style);
        set(gui, "renderWidth", Integer.valueOf(176));
        set(gui, "renderHeight", Integer.valueOf(166));

        invoke(
            gui,
            "initCustomButtons",
            new Class<?>[] {MMCEGuiExtConfig.FactoryController.class},
            new MMCEGuiExtConfig.FactoryController()
        );

        List<?> buttons = (List<?>) get(gui, "customButtons");
        assertEquals(1, buttons.size());
        assertNotNull(get(buttons.get(0), "button"));
        assertTrue(get(buttons.get(0), "button") instanceof GuiTexturedButton);
    }

    private static GuiFactoryControllerResizable allocateGui() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        return (GuiFactoryControllerResizable) unsafe.allocateInstance(GuiFactoryControllerResizable.class);
    }

    private static GuiFactoryControllerResizable allocateGuiWithRuntimeDefaults() throws Exception {
        GuiFactoryControllerResizable gui = allocateGui();
        set(gui, "customButtons", new ArrayList<Object>());
        set(gui, "customSmartEditors", new ArrayList<Object>());
        set(gui, "customSliders", new ArrayList<Object>());
        set(gui, "backgroundTextureLayers", new ArrayList<Object>());
        set(gui, "foregroundTextureLayers", new ArrayList<Object>());
        set(gui, "layerRuntimeStates", new HashMap<Object, Object>());
        set(gui, "textureLayerIds", new java.util.HashSet<Object>());
        set(gui, "subGuiStyleIndex", new HashMap<Object, Object>());
        set(gui, "styleOverride", MachineGuiStyleManager.ControllerStyle.EMPTY);
        set(gui, "smartInterfaceVirtualInputCache", new HashMap<String, String>());
        set(gui, "panelScroll", new HashMap<String, Integer>());
        set(gui, "panelMaxScroll", new HashMap<String, Integer>());
        set(gui, "activePageId", "main");
        return gui;
    }

    private static Object invoke(GuiFactoryControllerResizable target, String methodName, Object... args) throws Exception {
        Method method;
        if (args.length == 0) {
            method = GuiFactoryControllerResizable.class.getDeclaredMethod(methodName);
        } else {
            method = GuiFactoryControllerResizable.class.getDeclaredMethod(methodName, args[0].getClass());
        }
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Object invokeStatic(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = GuiFactoryControllerResizable.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    private static Object invoke(GuiFactoryControllerResizable target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = GuiFactoryControllerResizable.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Object newActiveSubGui(String mode) throws Exception {
        Class<?> runtimeStateClass = Class.forName("com.fushu.mmceguiext.client.gui.GuiFactoryControllerResizable$GuiRuntimeState");
        Constructor<?> runtimeStateConstructor = runtimeStateClass.getDeclaredConstructor();
        runtimeStateConstructor.setAccessible(true);
        Object runtimeState = runtimeStateConstructor.newInstance();
        Class<?> activeSubGuiClass = Class.forName("com.fushu.mmceguiext.client.gui.GuiFactoryControllerResizable$ActiveSubGui");
        Constructor<?> activeSubGuiConstructor = activeSubGuiClass.getDeclaredConstructor(String.class, String.class, runtimeStateClass);
        activeSubGuiConstructor.setAccessible(true);
        return activeSubGuiConstructor.newInstance("details", mode, runtimeState);
    }

    private static Object newCustomButton(GuiButton guiButton) throws Exception {
        Class<?> customButtonClass = Class.forName("com.fushu.mmceguiext.client.gui.GuiFactoryControllerResizable$CustomButton");
        Constructor<?> constructor = customButtonClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object customButton = constructor.newInstance();
        set(customButton, "button", guiButton);
        return customButton;
    }

    private static Object newSlider(String id, int x, int y, int width, int height) throws Exception {
        Class<?> sliderClass = Class.forName("com.fushu.mmceguiext.client.gui.GuiFactoryControllerResizable$CustomSlider");
        Constructor<?> constructor = sliderClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object slider = constructor.newInstance();
        set(slider, "id", id);
        set(slider, "key", id + "_key");
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
        set(slider, "foreground", Boolean.TRUE);
        set(slider, "visible", Boolean.TRUE);
        set(slider, "page", "main");
        return slider;
    }

    private static Object newHotkeyPageButton(String hotkey, String targetPage) throws Exception {
        Class<?> customButtonClass = Class.forName("com.fushu.mmceguiext.client.gui.GuiFactoryControllerResizable$CustomButton");
        Constructor<?> constructor = customButtonClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object customButton = constructor.newInstance();
        set(customButton, "action", "page");
        set(customButton, "targetPage", targetPage);
        set(customButton, "page", null);
        set(customButton, "hotkeys", new ArrayList<String>(Collections.singletonList(hotkey)));
        set(customButton, "consumeHotkey", Boolean.TRUE);
        return customButton;
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

    private static final class StyleProvider implements IMachineGuiStyleProvider {
        private final ResourceLocation styleKey;

        private StyleProvider(ResourceLocation styleKey) {
            this.styleKey = styleKey;
        }

        @Override
        public ResourceLocation getMachineControllerGuiStyle() {
            return this.styleKey;
        }
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

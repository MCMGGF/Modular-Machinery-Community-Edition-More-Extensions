package com.fushu.mmceguiext.common.integration.crafttweaker;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.common.registry.CustomCapacityCardRegistry;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.mc1120.events.ScriptRunEvent;
import crafttweaker.util.IEventHandler;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@ZenRegister
@ZenClass("mods.mmceguiext.MMCEGEEvents")
public final class MMCEGEEvents {
    private static final MMCEGEEvents INSTANCE = new MMCEGEEvents();
    private static final Object LOCK = new Object();
    private static final Map<String, List<IEventHandler<ControllerButtonClickEvent>>> BUTTON_CLICK_HANDLERS =
        new HashMap<String, List<IEventHandler<ControllerButtonClickEvent>>>();

    public MMCEGEEvents() {
    }

    public static MMCEGEEvents instance() {
        return INSTANCE;
    }

    @ZenMethod
    public static void onControllerButtonClick(String machineRegistryName, IEventHandler<ControllerButtonClickEvent> handler) {
        String key = normalizeMachineKey(machineRegistryName);
        if (key == null) {
            CraftTweakerAPI.logError("[MMCEME] onControllerButtonClick machineRegistryName is empty.");
            return;
        }
        if (handler == null) {
            CraftTweakerAPI.logError("[MMCEME] onControllerButtonClick handler is null for `" + machineRegistryName + "`.");
            return;
        }

        synchronized (LOCK) {
            addHandler(key, handler);
        }
    }

    public static void postControllerButtonClick(TileMultiblockMachineController controller, String buttonId) {
        List<IEventHandler<ControllerButtonClickEvent>> handlers = collectHandlers(controller);
        if (handlers.isEmpty()) {
            return;
        }

        ControllerButtonClickEvent event = new ControllerButtonClickEvent(controller, buttonId);
        for (IEventHandler<ControllerButtonClickEvent> handler : handlers) {
            try {
                handler.handle(event);
                if (event.isCanceled()) {
                    break;
                }
            } catch (Exception ex) {
                MMCEGuiExt.logger().warn("Caught an exception in MMCEME controller button handler.", ex);
            }
        }
        controller.markForUpdateSync();
    }

    @SubscribeEvent
    public void onScriptsPre(ScriptRunEvent.Pre event) {
        clear();
    }

    public static void clear() {
        clearButtonClickHandlers();
        CustomCapacityCardRegistry.clearScriptEntries();
    }

    static void clearButtonClickHandlers() {
        synchronized (LOCK) {
            BUTTON_CLICK_HANDLERS.clear();
        }
    }

    @ZenMethod
    public static void registerCapacityCard(String itemId, double multiplier) {
        registerCapacityCardAdvanced(itemId, 0, null, multiplier, 0L, 0L, false);
    }

    @ZenMethod
    public static void registerCapacityCardWithFlat(String itemId, double multiplier, long flatFluid, long flatGas) {
        registerCapacityCardAdvanced(itemId, 0, null, multiplier, flatFluid, flatGas, false);
    }

    @ZenMethod
    public static void registerCapacityCardAdvanced(String itemId,
                                                    int meta,
                                                    @Nullable String nbt,
                                                    double multiplier,
                                                    long flatFluid,
                                                    long flatGas,
                                                    boolean matchNbt) {
        if (itemId == null || itemId.trim().isEmpty()) {
            CraftTweakerAPI.logError("[MMCEME] registerCapacityCard itemId is empty.");
            return;
        }
        boolean ok = CustomCapacityCardRegistry.registerScriptEntry(
            "ct:" + itemId.trim().toLowerCase(Locale.ROOT) + ":" + Math.max(0, meta),
            itemId,
            meta,
            nbt,
            multiplier,
            flatFluid,
            flatGas,
            matchNbt
        );
        if (!ok) {
            CraftTweakerAPI.logError("[MMCEME] Failed to register capacity card for `" + itemId + "`.");
        }
    }

    private static void addHandler(String key, IEventHandler<ControllerButtonClickEvent> handler) {
        List<IEventHandler<ControllerButtonClickEvent>> handlers = BUTTON_CLICK_HANDLERS.get(key);
        if (handlers == null) {
            handlers = new ArrayList<IEventHandler<ControllerButtonClickEvent>>();
            BUTTON_CLICK_HANDLERS.put(key, handlers);
        }
        for (IEventHandler<ControllerButtonClickEvent> existing : handlers) {
            if (existing == handler) {
                return;
            }
        }
        handlers.add(handler);
    }

    private static List<IEventHandler<ControllerButtonClickEvent>> collectHandlers(TileMultiblockMachineController controller) {
        if (controller == null) {
            return Collections.emptyList();
        }

        DynamicMachine machine = controller.getFoundMachine();
        if (machine == null) {
            machine = controller.getBlueprintMachine();
        }
        if (machine == null || machine.getRegistryName() == null) {
            return Collections.emptyList();
        }

        String fullKey = machine.getRegistryName().toString().toLowerCase(Locale.ROOT);
        String pathKey = machine.getRegistryName().getPath().toLowerCase(Locale.ROOT);
        return collectHandlersForMachineKeys(fullKey, pathKey);
    }

    static List<IEventHandler<ControllerButtonClickEvent>> collectHandlersForMachineKeys(String fullKey, String pathKey) {
        List<IEventHandler<ControllerButtonClickEvent>> out =
            new ArrayList<IEventHandler<ControllerButtonClickEvent>>();
        Set<IEventHandler<ControllerButtonClickEvent>> seen = Collections.newSetFromMap(
            new IdentityHashMap<IEventHandler<ControllerButtonClickEvent>, Boolean>()
        );
        String normalizedFullKey = normalizeMachineKey(fullKey);
        String normalizedPathKey = normalizeMachineKey(pathKey);
        synchronized (LOCK) {
            appendHandlers(out, seen, normalizedFullKey);
            if (normalizedPathKey != null && !normalizedPathKey.equals(normalizedFullKey)) {
                appendHandlers(out, seen, normalizedPathKey);
            }
        }
        return out;
    }

    private static void appendHandlers(List<IEventHandler<ControllerButtonClickEvent>> out,
                                       Set<IEventHandler<ControllerButtonClickEvent>> seen,
                                       @Nullable String key) {
        if (key == null) {
            return;
        }
        List<IEventHandler<ControllerButtonClickEvent>> handlers = BUTTON_CLICK_HANDLERS.get(key);
        if (handlers != null && !handlers.isEmpty()) {
            for (IEventHandler<ControllerButtonClickEvent> handler : handlers) {
                if (seen.add(handler)) {
                    out.add(handler);
                }
            }
        }
    }

    @Nullable
    private static String normalizeMachineKey(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return null;
        }
        if (text.startsWith("<") && text.endsWith(">")) {
            text = text.substring(1, text.length() - 1).trim();
        }
        return text.isEmpty() ? null : text;
    }
}

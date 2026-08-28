package com.fushu.mmceguiext.common.integration.ae;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.MMCEGuiExtConfig;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Method;

@Mod.EventBusSubscriber(modid = MMCEGuiExt.MODID)
public final class ClassicAERegistryEventBridge {
    private static final String ITEM_INPUT_REGISTRY = "com.fushu.mmceguiext.common.registry.CustomAEItemInputBusGameRegistry";
    private static final String MIXED_INPUT_REGISTRY = "com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusGameRegistry";
    private static final String MIXED_OUTPUT_REGISTRY = "com.fushu.mmceguiext.common.registry.CustomAEMixedOutputBusGameRegistry";

    private ClassicAERegistryEventBridge() {
    }

    @SubscribeEvent
    public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        if (!isCustomAEBusRegistrationEnabled()) {
            return;
        }
        invoke(ITEM_INPUT_REGISTRY, "onRegisterBlocks", event);
        invoke(MIXED_INPUT_REGISTRY, "onRegisterBlocks", event);
        invoke(MIXED_OUTPUT_REGISTRY, "onRegisterBlocks", event);
    }

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        if (!isCustomAEBusRegistrationEnabled()) {
            return;
        }
        invoke(ITEM_INPUT_REGISTRY, "onRegisterItems", event);
        invoke(MIXED_INPUT_REGISTRY, "onRegisterItems", event);
        invoke(MIXED_OUTPUT_REGISTRY, "onRegisterItems", event);
    }

    private static void invoke(String className, String methodName, RegistryEvent.Register<?> event) {
        try {
            Class<?> registry = Class.forName(className);
            Method method = registry.getMethod(methodName, RegistryEvent.Register.class);
            method.invoke(null, event);
        } catch (Exception | LinkageError e) {
            MMCEGuiExt.logger().warn("Failed to dispatch classic AE registry event {} to {}: {}", methodName, className, e.toString());
        }
    }

    private static boolean isCustomAEBusRegistrationEnabled() {
        return MMCEGuiExtConfig.areCustomAEBusesEnabled() && AEIntegrationState.isClassicAEBusEnabled();
    }
}

package com.fushu.mmceguiext.client.registry;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.common.integration.ae.AEIntegrationState;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.reflect.Method;

@Mod.EventBusSubscriber(modid = MMCEGuiExt.MODID, value = Side.CLIENT)
public final class ClassicAEClientRegistryEventBridge {
    private static final String CLIENT_REGISTRY = "com.fushu.mmceguiext.client.registry.CustomAEBusClientRegistry";

    private ClassicAEClientRegistryEventBridge() {
    }

    @SubscribeEvent
    public static void onModelRegister(ModelRegistryEvent event) {
        if (!AEIntegrationState.isClassicAEBusEnabled()) {
            return;
        }
        invoke("onModelRegister", ModelRegistryEvent.class, event);
    }

    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        if (!AEIntegrationState.isClassicAEBusEnabled()) {
            return;
        }
        invoke("onModelBake", ModelBakeEvent.class, event);
    }

    private static void invoke(String methodName, Class<?> eventClass, Object event) {
        try {
            Class<?> registry = Class.forName(CLIENT_REGISTRY);
            Method method = registry.getMethod(methodName, eventClass);
            method.invoke(null, event);
        } catch (Exception | LinkageError e) {
            MMCEGuiExt.logger().warn("Failed to dispatch classic AE client registry event {}: {}", methodName, e.toString());
        }
    }
}

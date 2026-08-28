package com.fushu.mmceguiext.api.client.model;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;

/**
 * Client-side cache for baking a model with a caller-provided texture-slot map.
 * Downstream mods own their render-state selection and only use this class for
 * the common retexture-and-bake operation.
 */
@SideOnly(Side.CLIENT)
public final class RetexturedModelCache {
    private static final int MAX_ENTRIES = 256;
    private static final Set<RetexturedModelCache> INSTANCES =
        Collections.newSetFromMap(new WeakHashMap<RetexturedModelCache, Boolean>());
    private static boolean reloadListenerRegistered;

    private final Map<String, IBakedModel> cache =
        new LinkedHashMap<String, IBakedModel>(32, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, IBakedModel> eldest) {
                return size() > MAX_ENTRIES;
            }
        };
    private final Map<String, Boolean> failedKeys =
        new LinkedHashMap<String, Boolean>(32, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                return size() > MAX_ENTRIES;
            }
        };

    public RetexturedModelCache() {
        synchronized (INSTANCES) {
            INSTANCES.add(this);
        }
        ensureReloadListenerRegistered();
    }

    @Nullable
    public synchronized IBakedModel resolve(@Nullable ResourceLocation sourceModel,
                                            @Nullable Map<String, ResourceLocation> textureSlots) {
        return resolve(sourceModel, textureSlots, TRSRTransformation.identity(), "identity");
    }

    @Nullable
    public synchronized IBakedModel resolve(@Nullable ResourceLocation sourceModel,
                                            @Nullable Map<String, ResourceLocation> textureSlots,
                                            @Nullable IModelState transformation,
                                            @Nullable String transformationKey) {
        ensureReloadListenerRegistered();
        if (sourceModel == null) {
            return null;
        }
        Map<String, String> normalizedSlots = normalizeTextureSlots(textureSlots);
        IModelState resolvedTransformation = transformation == null
            ? TRSRTransformation.identity()
            : transformation;
        String cacheKey = sourceModel.toString() + "|" + normalizedSlots + "|"
            + (transformationKey == null ? "identity" : transformationKey);
        IBakedModel cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            net.minecraftforge.client.model.IModel model = ModelLoaderRegistry.getModel(sourceModel);
            if (!normalizedSlots.isEmpty()) {
                model = model.retexture(ImmutableMap.copyOf(normalizedSlots));
            }
            IBakedModel baked = model.bake(
                resolvedTransformation,
                DefaultVertexFormats.ITEM,
                location -> Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(location.toString())
            );
            cache.put(cacheKey, baked);
            return baked;
        } catch (Exception | LinkageError error) {
            if (failedKeys.put(cacheKey, Boolean.TRUE) == null) {
                MMCEGuiExt.logger().warn(
                    "Failed to bake retextured model '{}': {}",
                    sourceModel,
                    error.toString()
                );
            }
            return null;
        }
    }

    public synchronized void clear() {
        cache.clear();
        failedKeys.clear();
    }

    private static void ensureReloadListenerRegistered() {
        synchronized (INSTANCES) {
            if (reloadListenerRegistered) {
                return;
            }
            try {
                if (Minecraft.getMinecraft().getResourceManager() instanceof IReloadableResourceManager) {
                    ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager())
                        .registerReloadListener(resourceManager -> clearAll());
                    reloadListenerRegistered = true;
                }
            } catch (RuntimeException | LinkageError ignored) {
                // A later resolve call retries after the client resource manager is ready.
            }
        }
    }

    private static void clearAll() {
        for (RetexturedModelCache instance : snapshotInstances()) {
            instance.clear();
        }
    }

    private static ArrayList<RetexturedModelCache> snapshotInstances() {
        synchronized (INSTANCES) {
            return new ArrayList<RetexturedModelCache>(INSTANCES);
        }
    }

    private static Map<String, String> normalizeTextureSlots(@Nullable Map<String, ResourceLocation> textureSlots) {
        Map<String, String> out = new TreeMap<String, String>();
        if (textureSlots == null) {
            return out;
        }
        for (Map.Entry<String, ResourceLocation> entry : textureSlots.entrySet()) {
            String slot = entry.getKey();
            ResourceLocation texture = entry.getValue();
            if (slot == null || slot.trim().isEmpty() || texture == null) {
                continue;
            }
            out.put(slot.trim(), texture.toString());
        }
        return out;
    }
}

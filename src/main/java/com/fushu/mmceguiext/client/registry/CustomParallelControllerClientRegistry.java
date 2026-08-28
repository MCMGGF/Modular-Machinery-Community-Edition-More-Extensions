package com.fushu.mmceguiext.client.registry;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.core.MMCEGuiExtEarlyMixinLoader;
import hellfirepvp.modularmachinery.common.block.BlockParallelController;
import hellfirepvp.modularmachinery.common.block.prop.ParallelControllerData;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.item.Item;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Keeps the extra controller models available when another mod supplies a
 * blockparallelcontroller blockstate file under MMCE's resource namespace.
 */
@Mod.EventBusSubscriber(modid = MMCEGuiExt.MODID, value = Side.CLIENT)
public final class CustomParallelControllerClientRegistry {
    private static final ResourceLocation PARALLEL_CONTROLLER_LOCATION =
        new ResourceLocation("modularmachinery", "blockparallelcontroller");
    private static final String MODEL_PREFIX = "blockparallelcontroller_";

    private CustomParallelControllerClientRegistry() {
    }

    @SubscribeEvent
    public static void onModelRegister(ModelRegistryEvent event) {
        if (!MMCEGuiExtEarlyMixinLoader.areCustomParallelControllerTiersEnabled()) {
            return;
        }

        Block block = Block.REGISTRY.getObject(PARALLEL_CONTROLLER_LOCATION);
        if (!(block instanceof BlockParallelController)) {
            return;
        }

        ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
            @Override
            protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                return resolveStateModelLocation(state);
            }
        });

        registerCustomItemModels(Item.getItemFromBlock(block));
    }

    private static ModelResourceLocation resolveStateModelLocation(IBlockState state) {
        String typeName = "normal";
        if (state != null) {
            for (IProperty<?> property : state.getPropertyKeys()) {
                if (!"type".equals(property.getName())) {
                    continue;
                }
                Comparable<?> value = state.getValue(property);
                if (value instanceof IStringSerializable) {
                    typeName = ((IStringSerializable) value).getName();
                } else if (value != null) {
                    typeName = value.toString().toLowerCase(java.util.Locale.ROOT);
                }
                break;
            }
        }

        ResourceLocation model = new ResourceLocation(
            "modularmachinery",
            MODEL_PREFIX + typeName
        );
        return new ModelResourceLocation(model, "normal");
    }

    private static void registerCustomItemModels(Item item) {
        if (item == null) {
            return;
        }

        for (ParallelControllerData data : ParallelControllerData.values()) {
            String name = data.getName();
            if (!name.startsWith("mmceme_")) {
                continue;
            }

            int tierIndex;
            try {
                tierIndex = Integer.parseInt(name.substring("mmceme_".length()));
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (tierIndex < 0
                || tierIndex >= MMCEGuiExtEarlyMixinLoader.getCustomParallelControllerTierCount()) {
                continue;
            }

            ResourceLocation model = new ResourceLocation(
                "modularmachinery",
                MODEL_PREFIX + name
            );
            ModelBakery.registerItemVariants(item, model);
            ModelLoader.setCustomModelResourceLocation(
                item,
                data.ordinal(),
                new ModelResourceLocation(model, "inventory")
            );
        }
    }
}

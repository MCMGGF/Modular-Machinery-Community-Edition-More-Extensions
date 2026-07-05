package com.fushu.mmceguiext.common.integration.ae;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.MMCEGuiExtConfig;
import com.fushu.mmceguiext.common.block.BlockCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.block.BlockCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.block.BlockCustomMEItemInputBus;
import com.fushu.mmceguiext.common.container.ContainerCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.container.ContainerCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.container.ContainerCustomMEItemInputBus;
import com.fushu.mmceguiext.common.network.PktCustomAEMixedSlotUpdate;
import com.fushu.mmceguiext.common.network.PktCustomMEItemInputBusInvAction;
import com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry;
import com.fushu.mmceguiext.common.registry.CustomAEMixedOutputBusRegistry;
import com.fushu.mmceguiext.common.registry.CustomAEItemInputBusRegistry;
import com.fushu.mmceguiext.common.registry.CustomCapacityCardRegistry;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.tile.TileCustomMEItemInputBus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;

public final class ClassicAEIntegration {
    private ClassicAEIntegration() {
    }

    public static int registerNetworkMessages(SimpleNetworkWrapper channel, int nextPacketId) {
        if (!MMCEGuiExtConfig.areCustomAEBusesEnabled()) {
            return nextPacketId;
        }
        channel.registerMessage(
            PktCustomAEMixedSlotUpdate.class,
            PktCustomAEMixedSlotUpdate.class,
            nextPacketId++,
            Side.SERVER
        );
        channel.registerMessage(
            PktCustomMEItemInputBusInvAction.class,
            PktCustomMEItemInputBusInvAction.class,
            nextPacketId++,
            Side.SERVER
        );
        return nextPacketId;
    }

    public static void loadDefinitions() {
        if (!MMCEGuiExtConfig.areCustomAEBusesEnabled()) {
            CustomAEItemInputBusRegistry.clear();
            CustomAEMixedInputBusRegistry.clear();
            CustomAEMixedOutputBusRegistry.clear();
            return;
        }
        CustomAEItemInputBusRegistry.loadAll();
        CustomAEMixedInputBusRegistry.loadAll();
        CustomAEMixedOutputBusRegistry.loadAll();
        CustomCapacityCardRegistry.loadAll();
    }

    @Nullable
    public static Object getServerGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (id == MMCEGuiExt.GUI_CUSTOM_AE_ITEM_INPUT) {
            if (!(tileEntity instanceof TileCustomMEItemInputBus)) {
                return null;
            }
            TileCustomMEItemInputBus tile = (TileCustomMEItemInputBus) tileEntity;
            resolveCustomItemInputBusDef(world, pos, tile);
            return new ContainerCustomMEItemInputBus(tile, player);
        }
        if (id == MMCEGuiExt.GUI_CUSTOM_AE_MIXED_INPUT) {
            if (!(tileEntity instanceof TileCustomAEMixedInputBus)) {
                return null;
            }
            TileCustomAEMixedInputBus tile = (TileCustomAEMixedInputBus) tileEntity;
            CustomAEMixedInputBusRegistry.Def def = resolveMixedInputBusDef(world, pos, tile);
            if (def == null) {
                MMCEGuiExt.logger().warn("Falling back to a safe mixed input GUI layout at {} because the definition could not be resolved.", pos);
                buildFallbackMixedInputBusDef(tile);
            }
            return new ContainerCustomAEMixedInputBus(tile, player);
        }
        if (id == MMCEGuiExt.GUI_CUSTOM_AE_MIXED_OUTPUT) {
            if (!(tileEntity instanceof TileCustomAEMixedOutputBus)) {
                return null;
            }
            TileCustomAEMixedOutputBus tile = (TileCustomAEMixedOutputBus) tileEntity;
            CustomAEMixedOutputBusRegistry.Def def = resolveMixedOutputBusDef(world, pos, tile);
            return def == null ? null : new ContainerCustomAEMixedOutputBus(tile, player);
        }
        return null;
    }

    @Nullable
    public static Object getClientGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (id == MMCEGuiExt.GUI_CUSTOM_AE_ITEM_INPUT) {
            if (!(tileEntity instanceof TileCustomMEItemInputBus)) {
                return null;
            }
            TileCustomMEItemInputBus tile = (TileCustomMEItemInputBus) tileEntity;
            resolveCustomItemInputBusDef(world, pos, tile);
            return createClientGui(
                "com.fushu.mmceguiext.client.gui.GuiMEItemInputBusCustom",
                new Class<?>[]{TileCustomMEItemInputBus.class, EntityPlayer.class},
                tile,
                player
            );
        }
        if (id == MMCEGuiExt.GUI_CUSTOM_AE_MIXED_INPUT) {
            if (!(tileEntity instanceof TileCustomAEMixedInputBus)) {
                return null;
            }
            TileCustomAEMixedInputBus tile = (TileCustomAEMixedInputBus) tileEntity;
            CustomAEMixedInputBusRegistry.Def def = resolveMixedInputBusDef(world, pos, tile);
            if (def == null) {
                MMCEGuiExt.logger().warn("Falling back to a safe mixed input client GUI at {} because the definition could not be resolved.", pos);
                def = buildFallbackMixedInputBusDef(tile);
            }
            return createClientGui(
                "com.fushu.mmceguiext.client.gui.GuiCustomAEMixedInputBus",
                new Class<?>[]{TileCustomAEMixedInputBus.class, EntityPlayer.class, CustomAEMixedInputBusRegistry.Def.class},
                tile,
                player,
                def
            );
        }
        if (id == MMCEGuiExt.GUI_CUSTOM_AE_MIXED_OUTPUT) {
            if (!(tileEntity instanceof TileCustomAEMixedOutputBus)) {
                return null;
            }
            TileCustomAEMixedOutputBus tile = (TileCustomAEMixedOutputBus) tileEntity;
            CustomAEMixedOutputBusRegistry.Def def = resolveMixedOutputBusDef(world, pos, tile);
            return def == null ? null : createClientGui(
                "com.fushu.mmceguiext.client.gui.GuiCustomAEMixedOutputBus",
                new Class<?>[]{TileCustomAEMixedOutputBus.class, EntityPlayer.class, CustomAEMixedOutputBusRegistry.Def.class},
                tile,
                player,
                def
            );
        }
        return null;
    }

    @Nullable
    private static Object createClientGui(String className, Class<?>[] signature, Object... args) {
        try {
            return Class.forName(className).getConstructor(signature).newInstance(args);
        } catch (Exception | LinkageError e) {
            MMCEGuiExt.logger().warn("Failed to create GUI {}: {}", className, e.toString());
            return null;
        }
    }

    @Nullable
    private static CustomAEItemInputBusRegistry.Def resolveCustomItemInputBusDef(World world, BlockPos pos, TileCustomMEItemInputBus tile) {
        CustomAEItemInputBusRegistry.Def def = tile.getDefinition();
        if (def != null) {
            return def;
        }
        if (world.getBlockState(pos).getBlock() instanceof BlockCustomMEItemInputBus) {
            BlockCustomMEItemInputBus block = (BlockCustomMEItemInputBus) world.getBlockState(pos).getBlock();
            tile.setDefinitionId(block.getRegistryName() == null ? null : block.getRegistryName().toString());
            return block.getDefinition();
        }
        return null;
    }

    @Nullable
    private static CustomAEMixedInputBusRegistry.Def resolveMixedInputBusDef(World world, BlockPos pos, TileCustomAEMixedInputBus tile) {
        CustomAEMixedInputBusRegistry.Def def = tile.getDefinition();
        if (def != null) {
            return def;
        }
        if (world.getBlockState(pos).getBlock() instanceof BlockCustomAEMixedInputBus) {
            BlockCustomAEMixedInputBus block = (BlockCustomAEMixedInputBus) world.getBlockState(pos).getBlock();
            block.ensureDefinitionId(tile);
            return block.getDefinition();
        }
        return null;
    }

    @Nullable
    private static CustomAEMixedOutputBusRegistry.Def resolveMixedOutputBusDef(World world, BlockPos pos, TileCustomAEMixedOutputBus tile) {
        CustomAEMixedOutputBusRegistry.Def def = tile.getDefinition();
        if (def != null) {
            return def;
        }
        if (world.getBlockState(pos).getBlock() instanceof BlockCustomAEMixedOutputBus) {
            BlockCustomAEMixedOutputBus block = (BlockCustomAEMixedOutputBus) world.getBlockState(pos).getBlock();
            block.ensureDefinitionId(tile);
            return block.getDefinition();
        }
        return null;
    }

    private static CustomAEMixedInputBusRegistry.Def buildFallbackMixedInputBusDef(TileCustomAEMixedInputBus tile) {
        CustomAEMixedInputBusRegistry.Def def = new CustomAEMixedInputBusRegistry.Def();
        def.id = tile.getDefinitionId();
        def.playerInventoryX = 8;
        def.playerInventoryY = 141;
        def.guiWidth = 176;
        def.guiHeight = 235;
        def.backgroundTextureWidth = 176;
        def.backgroundTextureHeight = 235;
        def.configSlots = buildFallbackSlotPoints(tile.getConfigInventory().getSlots(), 8, 17, 18);
        def.storageSlots = buildFallbackSlotPoints(tile.getInternalInventory().getSlots(), 8, 53, 18);
        def.capacityCardSlots = buildFallbackSlotPoints(tile.getCapacityCardInventory().getSlots(), 8, 89, 18);
        def.fluidConfigTanks = java.util.Collections.emptyList();
        def.gasConfigTanks = java.util.Collections.emptyList();
        def.fluidStorageTanks = java.util.Collections.emptyList();
        def.gasStorageTanks = java.util.Collections.emptyList();
        def.gui = new CustomAEMixedInputBusRegistry.GuiDef();
        def.gui.width = def.guiWidth;
        def.gui.height = def.guiHeight;
        def.gui.components = java.util.Collections.emptyList();
        return def;
    }

    private static java.util.List<CustomAEMixedInputBusRegistry.SlotPoint> buildFallbackSlotPoints(int slotCount, int startX, int startY, int spacingX) {
        java.util.List<CustomAEMixedInputBusRegistry.SlotPoint> points =
            new java.util.ArrayList<CustomAEMixedInputBusRegistry.SlotPoint>(Math.max(0, slotCount));
        for (int i = 0; i < slotCount; i++) {
            CustomAEMixedInputBusRegistry.SlotPoint point = new CustomAEMixedInputBusRegistry.SlotPoint();
            point.x = startX + i * spacingX;
            point.y = startY;
            points.add(point);
        }
        return points;
    }
}

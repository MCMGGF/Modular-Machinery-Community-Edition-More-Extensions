package com.fushu.mmceguiext;

import com.fushu.mmceguiext.common.energy.LongEnergyCapability;
import com.fushu.mmceguiext.common.block.BlockCustomHatch;
import com.fushu.mmceguiext.common.container.ContainerFluidProcessorHatchCustom;
import com.fushu.mmceguiext.common.integration.ae.AEIntegrationState;
import com.fushu.mmceguiext.common.registry.CustomHatchRegistry;
import com.fushu.mmceguiext.common.integration.crafttweaker.MMCEGEEvents;
import com.fushu.mmceguiext.common.integration.cfn.CFNEnergyIntegration;
import com.fushu.mmceguiext.common.network.PktControllerButtonAction;
import com.fushu.mmceguiext.common.network.PktControllerCustomDataSync;
import com.fushu.mmceguiext.common.network.PktCustomHatchEnergySync;
import com.fushu.mmceguiext.common.tile.TileCustomHatch;
import com.fushu.mmceguiext.common.network.PktControllerSmartInterfaceUpdate;
import hellfirepvp.modularmachinery.common.base.Mods;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

@Mod(
    modid = MMCEGuiExt.MODID,
    name = MMCEGuiExt.NAME,
    version = MMCEGuiExt.VERSION,
    dependencies = "required-after:modularmachinery;after:appliedenergistics2;after:ae2;required-after:mekanism;after:mekeng"
)
public class MMCEGuiExt {
    public static final String MODID = "mmceguiext";
    private static final Logger LOGGER = LogManager.getLogger(MODID);
    public static final String NAME = "Modular Machinery: Community Edition Gui Edit";
    public static final String VERSION = "1.3.4";
    public static final int GUI_CUSTOM_HATCH = 1;
    public static final int GUI_CUSTOM_AE_MIXED_INPUT = 2;
    public static final int GUI_CUSTOM_AE_MIXED_OUTPUT = 3;
    public static final int GUI_CUSTOM_AE_ITEM_INPUT = 4;
    public static final SimpleNetworkWrapper NET_CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
    private static int nextPacketId = 0;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LongEnergyCapability.register();
        NET_CHANNEL.registerMessage(
            PktControllerSmartInterfaceUpdate.class,
            PktControllerSmartInterfaceUpdate.class,
            nextPacketId++,
            Side.SERVER
        );
        NET_CHANNEL.registerMessage(
            PktControllerButtonAction.class,
            PktControllerButtonAction.class,
            nextPacketId++,
            Side.SERVER
        );
        NET_CHANNEL.registerMessage(
            PktCustomHatchEnergySync.class,
            PktCustomHatchEnergySync.class,
            nextPacketId++,
            Side.CLIENT
        );
        NET_CHANNEL.registerMessage(
            PktControllerCustomDataSync.class,
            PktControllerCustomDataSync.class,
            nextPacketId++,
            Side.CLIENT
        );
        if (MMCEGuiExtConfig.areCustomHatchesEnabled()) {
            CustomHatchRegistry.loadAll();
        } else {
            CustomHatchRegistry.clear();
        }
        initClassicAEIntegration();
        if (event.getSide().isClient()) {
            initializeMouseTweaksClassLoaderGuard();
            preloadClientStyleCache();
            registerClientGuiEventHandler();
        }
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        CFNEnergyIntegration.registerIfPresent();
        if (Mods.TOP.isPresent()) {
            registerTopProvider();
        }
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        if (Mods.CRAFTTWEAKER.isPresent()) {
            registerCraftTweakerEventBridge();
        }
    }

    public static Logger logger() {
        return LOGGER;
    }

    private static void initializeMouseTweaksClassLoaderGuard() {
        try {
            Class.forName("com.fushu.mmceguiext.client.compat.MouseTweaksClassLoaderGuard")
                .getMethod("initialize")
                .invoke(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.warn("Failed to initialize optional Mouse Tweaks class-loader guard: {}", e.toString());
        }
    }

    private static void initClassicAEIntegration() {
        if (!MMCEGuiExtConfig.areCustomAEBusesEnabled()) {
            LOGGER.info("JSON-defined custom AE buses are disabled by config; classic AE custom bus integration will not register blocks.");
            return;
        }
        if (!AEIntegrationState.isClassicAEBusEnabled()) {
            if (AEIntegrationState.isAE2SPresent()) {
                LOGGER.info("AE2S detected. Classic appeng-based custom AE buses are disabled until native AE2S bus support is implemented.");
            } else if (AEIntegrationState.isClassicAE2Present()) {
                LOGGER.warn("Classic AE2 detected without Mekanism Energistics. Custom AE buses are disabled.");
            } else {
                LOGGER.info("No classic AE2 implementation detected. Custom AE buses are disabled.");
            }
            return;
        }

        try {
            Class<?> integration = Class.forName("com.fushu.mmceguiext.common.integration.ae.ClassicAEIntegration");
            nextPacketId = ((Integer) integration
                .getMethod("registerNetworkMessages", SimpleNetworkWrapper.class, int.class)
                .invoke(null, NET_CHANNEL, nextPacketId)).intValue();
            integration.getMethod("loadDefinitions").invoke(null);
            LOGGER.info("Classic AE2 custom bus integration enabled.");
        } catch (Exception | LinkageError e) {
            LOGGER.warn("Failed to initialize classic AE2 custom bus integration: {}", e.toString());
        }
    }

    private static class GuiHandler implements IGuiHandler {
        @Override
        public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!canOpenTileGui(player, world, pos)) {
                return null;
            }
            TileEntity tileEntity = world.getTileEntity(pos);
            if (id == GUI_CUSTOM_AE_ITEM_INPUT) {
                return createClassicAEGuiElement("getServerGuiElement", id, player, world, pos);
            }
            if (id == GUI_CUSTOM_AE_MIXED_INPUT) {
                return createClassicAEGuiElement("getServerGuiElement", id, player, world, pos);
            }
            if (id == GUI_CUSTOM_AE_MIXED_OUTPUT) {
                return createClassicAEGuiElement("getServerGuiElement", id, player, world, pos);
            }
            if (id != GUI_CUSTOM_HATCH) {
                return null;
            }
            if (!(tileEntity instanceof TileCustomHatch)) {
                return null;
            }
            TileCustomHatch tile = (TileCustomHatch) tileEntity;
            com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef def = resolveCustomHatchDef(world, pos, tile);
            if (def == null) {
                LOGGER.warn("Cannot open custom hatch GUI at {}: missing definition, tile id={}.", pos, tile.getDefinitionId());
                return null;
            }
            return new ContainerFluidProcessorHatchCustom(tile, player, def);
        }

        @Override
        public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!canOpenTileGui(player, world, pos)) {
                return null;
            }
            TileEntity tileEntity = world.getTileEntity(pos);
            if (id == GUI_CUSTOM_AE_ITEM_INPUT) {
                return createClassicAEGuiElement("getClientGuiElement", id, player, world, pos);
            }
            if (id == GUI_CUSTOM_AE_MIXED_INPUT) {
                return createClassicAEGuiElement("getClientGuiElement", id, player, world, pos);
            }
            if (id == GUI_CUSTOM_AE_MIXED_OUTPUT) {
                return createClassicAEGuiElement("getClientGuiElement", id, player, world, pos);
            }
            if (id != GUI_CUSTOM_HATCH) {
                return null;
            }
            if (!(tileEntity instanceof TileCustomHatch)) {
                return null;
            }
            TileCustomHatch tile = (TileCustomHatch) tileEntity;
            com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef def = resolveCustomHatchDef(world, pos, tile);
            if (def == null) {
                LOGGER.warn("Cannot create custom hatch client GUI at {}: missing definition, tile id={}.", pos, tile.getDefinitionId());
                return null;
            }
            return createClientGui(
                "com.fushu.mmceguiext.client.gui.GuiFluidProcessorHatchCustom",
                new Class<?>[]{TileEntity.class, EntityPlayer.class, com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef.class},
                tile,
                player,
                def
            );
        }

        private boolean canOpenTileGui(EntityPlayer player, World world, BlockPos pos) {
            return player != null
                && world != null
                && pos != null
                && world.isBlockLoaded(pos)
                && player.world == world
                && player.getDistanceSqToCenter(pos) <= 64D;
        }

        @Nullable
        private Object createClientGui(String className, Class<?>[] signature, Object... args) {
            try {
                return Class.forName(className).getConstructor(signature).newInstance(args);
            } catch (Exception e) {
                LOGGER.warn("Failed to create GUI {}: {}", className, e.toString());
                return null;
            }
        }

        @Nullable
        private Object createClassicAEGuiElement(String methodName, int id, EntityPlayer player, World world, BlockPos pos) {
            if (!MMCEGuiExtConfig.areCustomAEBusesEnabled() || !AEIntegrationState.isClassicAEBusEnabled()) {
                return null;
            }
            try {
                return Class.forName("com.fushu.mmceguiext.common.integration.ae.ClassicAEIntegration")
                    .getMethod(methodName, int.class, EntityPlayer.class, World.class, BlockPos.class)
                    .invoke(null, id, player, world, pos);
            } catch (Exception | LinkageError e) {
                LOGGER.warn("Failed to create classic AE GUI element {} for id {}: {}", methodName, id, e.toString());
                return null;
            }
        }

        @Nullable
        private com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef resolveCustomHatchDef(World world, net.minecraft.util.math.BlockPos pos, TileCustomHatch tile) {
            com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef def = tile.getDefinition();
            if (def != null) {
                return def;
            }
            if (world.getBlockState(pos).getBlock() instanceof BlockCustomHatch) {
                BlockCustomHatch block = (BlockCustomHatch) world.getBlockState(pos).getBlock();
                tile.setDefinitionId(block.getRegistryName() == null ? null : block.getRegistryName().toString());
                return block.getDefinition();
            }
            return null;
        }

    }

    private static void preloadClientStyleCache() {
        if (!MMCEGuiExtConfig.novaEngCoreCompatibilityMode) {
            return;
        }
        try {
            Class.forName("com.fushu.mmceguiext.client.config.MachineGuiStyleManager")
                .getMethod("preloadAndPinCache")
                .invoke(null);
        } catch (Exception ignored) {
        }
    }

    private static void registerClientGuiEventHandler() {
        try {
            MinecraftForge.EVENT_BUS.register(Class.forName("com.fushu.mmceguiext.client.ClientGuiEventHandler").newInstance());
        } catch (Exception | LinkageError ignored) {
        }
    }

    private static void registerTopProvider() {
        try {
            Object top = Class.forName("mcjty.theoneprobe.TheOneProbe").getField("theOneProbeImp").get(null);
            top.getClass()
                .getMethod("registerProvider", Class.forName("mcjty.theoneprobe.api.IProbeInfoProvider"))
                .invoke(top, Class.forName("com.fushu.mmceguiext.common.integration.theoneprobe.CustomHatchInfoProvider").newInstance());
        } catch (Exception | LinkageError ignored) {
        }
    }

    private static void registerCraftTweakerEventBridge() {
        try {
            MinecraftForge.EVENT_BUS.register(MMCEGEEvents.instance());
        } catch (Exception | LinkageError ignored) {
        }
    }
}


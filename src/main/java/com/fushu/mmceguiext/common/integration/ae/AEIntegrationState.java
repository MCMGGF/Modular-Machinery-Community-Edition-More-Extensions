package com.fushu.mmceguiext.common.integration.ae;

import net.minecraftforge.fml.common.Loader;

public final class AEIntegrationState {
    public static final String CLASSIC_AE2_MODID = "appliedenergistics2";
    public static final String AE2S_MODID = "ae2";
    public static final String MEKENG_MODID = "mekeng";

    private AEIntegrationState() {
    }

    public static boolean isClassicAE2Present() {
        return Loader.isModLoaded(CLASSIC_AE2_MODID)
            && isClassPresent("appeng.api.AEApi")
            && isClassPresent("appeng.me.helpers.AENetworkProxy");
    }

    public static boolean isAE2SPresent() {
        return Loader.isModLoaded(AE2S_MODID)
            && isClassPresent("ae2.api.networking.IGridNode")
            && isClassPresent("ae2.api.storage.cells.IBasicCellItem");
    }

    public static boolean isMekanismEnergisticsPresent() {
        return Loader.isModLoaded(MEKENG_MODID)
            && isClassPresent("com.mekeng.github.common.me.storage.IGasStorageChannel");
    }

    public static boolean isClassicAEBusEnabled() {
        return isClassicAE2Present() && isMekanismEnergisticsPresent();
    }

    public static boolean isAnyAEPresent() {
        return isClassicAE2Present() || isAE2SPresent();
    }

    public static String describeLoadedAE() {
        if (isClassicAE2Present()) {
            return CLASSIC_AE2_MODID;
        }
        if (isAE2SPresent()) {
            return AE2S_MODID;
        }
        return "none";
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, AEIntegrationState.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }
}

package com.fushu.mmceguiext;

/**
 * Stable version marker for downstream MMCEGE integrations.
 */
public final class MMCEGuiExtApi {
    public static final int API_LEVEL = 1;

    private MMCEGuiExtApi() {
    }

    public static boolean isApiLevelAtLeast(int requiredLevel) {
        return requiredLevel >= 0 && API_LEVEL >= requiredLevel;
    }
}

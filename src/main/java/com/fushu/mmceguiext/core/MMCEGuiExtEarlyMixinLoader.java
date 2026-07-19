package com.fushu.mmceguiext.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class MMCEGuiExtEarlyMixinLoader implements IFMLLoadingPlugin {
    static final List<String> ALWAYS_REGISTERED_MIXIN_CONFIGS = Collections.unmodifiableList(
        Collections.singletonList("mixins.mmceguiext.json")
    );

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(final Map<String, Object> data) {
        for (String config : ALWAYS_REGISTERED_MIXIN_CONFIGS) {
            Mixins.addConfiguration(config);
        }
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

}

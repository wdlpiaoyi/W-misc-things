package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.feature.FeatureRegistry;
import com.wdlpiaoyi.wmisc.feature.WmiscFeature;
import com.wdlpiaoyi.wmisc.util.ConfigPeek;
import java.util.List;
import java.util.Map;
import net.minecraftforge.common.ForgeConfigSpec;

public final class GoeticGgtuDimensionFeature implements WmiscFeature {
    private static final String SECTION = "features.goetic_ggtu_dimension";
    private static ForgeConfigSpec.BooleanValue disableFrameGen;
    private static ForgeConfigSpec.BooleanValue disableUpscaling;
    private static volatile boolean active;

    public String id() {
        return "goetic_ggtu_dimension";
    }

    public String summary() {
        return "个人/心灵维度里暂停 GGTU 的帧生成与超分辨率：修 skybox/边界墙闪烁与超分穿透（代价是该维度没有 GGTU）";
    }

    public List<String> requiredMods() {
        return List.of("goeticlegacy", "resolutionscaler");
    }

    public void defineOptions(ForgeConfigSpec.Builder builder) {
        disableFrameGen = builder.comment("这两个维度里关闭 GGTU 帧生成（墙/天空在合成帧里消失、按倍率闪烁的来源）").define("disable_frame_gen", true);
        disableUpscaling = builder.comment("这两个维度里关闭 GGTU 超分辨率（超分时主目标深度被掏空，AFTER_LEVEL 的天空/墙会穿透方块）").define("disable_upscaling", true);
    }

    public void onClientSetup() {
        active = true;
        Wmisc.LOG.info("[w_misc] Goetic/GGTU 维度闸门：个人/心灵维度内 帧生成 {}、超分辨率 {}", onOff(disableFrameGen()), onOff(disableUpscaling()));
    }

    public void onConfigReload(Map<String, String> values) {
        ConfigPeek.applyBoolean(values, "disable_frame_gen", disableFrameGen);
        ConfigPeek.applyBoolean(values, "disable_upscaling", disableUpscaling);
    }

    public static boolean isActive() {
        return active && FeatureRegistry.liveActive("goetic_ggtu_dimension");
    }

    public static boolean disableFrameGen() {
        return read(disableFrameGen, true);
    }

    public static boolean disableUpscaling() {
        return read(disableUpscaling, true);
    }

    public static String section() {
        return "features.goetic_ggtu_dimension";
    }

    private static String onOff(boolean value) {
        return value ? "暂停" : "保持 GGTU";
    }

    private static boolean read(ForgeConfigSpec.BooleanValue value, boolean fallback) {
        if (value == null) {
            return fallback;
        } else {
            try {
                return (Boolean)value.get();
            } catch (IllegalStateException var3) {
                return fallback;
            }
        }
    }
}

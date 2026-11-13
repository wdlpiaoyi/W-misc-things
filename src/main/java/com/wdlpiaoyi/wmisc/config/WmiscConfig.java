package com.wdlpiaoyi.wmisc.config;

import com.wdlpiaoyi.wmisc.feature.FeatureRegistry;
import com.wdlpiaoyi.wmisc.feature.Features;
import com.wdlpiaoyi.wmisc.feature.WmiscFeature;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * 模组配置。
 *
 * <pre>
 * [general]
 *   debug_log = false
 * [features.&lt;模块 id&gt;]
 *   enabled = true
 *   ...模块自己的选项
 * </pre>
 */
public final class WmiscConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue DEBUG_LOG;

    private WmiscConfig() {
    }

    static {
        // 保证模块清单已经登记，配置段才能完整生成。
        Features.init();

        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("w_misc 通用选项").push("general");
        DEBUG_LOG = builder.comment("输出额外调试日志").define("debug_log", false);
        builder.pop();

        builder.comment(
                "各功能模块开关。模块 id 见启动日志里的一览表。",
                "前置模组缺失时该模块会自动跳过，与这里的开关无关。").push("features");
        for (WmiscFeature feature : FeatureRegistry.all()) {
            builder.comment(feature.summary().isEmpty() ? feature.id() : feature.summary());
            builder.push(feature.id());
            ForgeConfigSpec.BooleanValue toggle = builder.define("enabled", feature.defaultEnabled());
            FeatureRegistry.bindToggle(feature.id(), toggle);
            feature.defineOptions(builder);
            builder.pop();
        }
        builder.pop();

        SPEC = builder.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "w_misc-common.toml");
    }
}

package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.feature.WmiscFeature;
import com.wdlpiaoyi.wmisc.util.ConfigPeek;
import com.wdlpiaoyi.wmisc.compat.chloride.ChlorideJeiPatch;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * 把 chloride 1.8.1 × JEI 15.70+ 的兼容补丁并进本模组（替代实例里单独的 chloridejeicompat jar）。
 *
 * <p>做法：chloride 的 {@code chloride.mixin.json} 无条件注册了一条 {@code jei_rei_emi.JeiOverlayMixin}，
 * 它的 @Shadow 目标在 JEI 15.70+ 被删除，且配置 {@code required:true}，于是进世界就崩。
 * 本模块在 Mixin 运行时里把这一条摘掉（三重兜底，纯反射），chloride 其它 mixin 不动，
 * 也不改 chloride / JEI 的 jar。</p>
 *
 * <p>版本门控：只对 chloride 1.8.1 + JEI ≥ 15.70.0.221 动手；其它组合只打一行日志。</p>
 */
public final class ChlorideJeiFeature implements WmiscFeature {
    private static final String SECTION = "features.chloride_jei_bundle";

    @Override
    public String id() {
        return "chloride_jei_bundle";
    }

    @Override
    public String summary() {
        return "并入 chloride 1.8.1 × JEI 15.70+ 兼容补丁，替代独立兼容 jar";
    }

    @Override
    public boolean implemented() {
        return true;
    }

    @Override
    public List<String> requiredMods() {
        return List.of("chloride", "jei");
    }

    @Override
    public void defineOptions(ForgeConfigSpec.Builder builder) {
        builder.comment("跳过版本门控，只在别的整合包上排查时用")
                .define("force", false);
        builder.comment("打印 Mixin 变换器、配置名单等诊断信息")
                .define("verbose", false);
    }

    @Override
    public void onConstruct() {
        // 构造期 Forge 配置还没加载，直接读文件判断开关。
        if (ConfigPeek.getBoolean(SECTION, "enabled", true)) {
            ChlorideJeiPatch.attempt("mod-constructor");
        }
    }

    @Override
    public void onCommonSetup() {
        ChlorideJeiPatch.attempt("common-setup");
    }

    @Override
    public void onClientSetup() {
        ChlorideJeiPatch.attempt("client-setup");
    }
}

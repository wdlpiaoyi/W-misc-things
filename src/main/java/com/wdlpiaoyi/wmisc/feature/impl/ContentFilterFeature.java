package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.feature.WmiscFeature;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 内容过滤：按规则屏蔽模组里与玩法无关的宣传性文本 / 贴图。
 *
 * <p>TODO：规则来源（内置清单 / 数据包 json）、作用层面（资源重载 / 文本组件拦截 /
 * 物品与 GUI 渲染）待定。先留开关与审计模式，实现时再补规则集。</p>
 */
public final class ContentFilterFeature implements WmiscFeature {
    @Override
    public String id() {
        return "content_filter";
    }

    @Override
    public String summary() {
        return "按配置规则过滤模组注入的内容";
    }

    @Override
    public boolean implemented() {
        return false;
    }

    @Override
    public void defineOptions(ForgeConfigSpec.Builder builder) {
        builder.comment("只记录命中的规则、不实际拦截（调试用）")
                .define("audit_only", false);
    }
}

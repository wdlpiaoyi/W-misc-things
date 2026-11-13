package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.feature.WmiscFeature;

import java.util.List;

/**
 * 创造模式下忽略 enigmatic legacy 七咒之戒的佩戴时长条件，让相关物品可以直接用。
 *
 * <p>TODO：确认 enigmatic legacy 的 modid 与「佩戴时长」的判定点（属性修饰 / 事件 /
 * Curios 槽位检查），再决定是 mixin 还是事件拦截。</p>
 */
public final class EnigmaticCreativeFeature implements WmiscFeature {
    @Override
    public String id() {
        return "enigmatic_legacy_creative";
    }

    @Override
    public String summary() {
        return "创造模式下无视七咒之戒的佩戴时长条件";
    }

    @Override
    public boolean implemented() {
        return false;
    }

    @Override
    public List<String> requiredMods() {
        // TODO: 核对 modid 后再启用前置判定，避免 modid 写错导致模块永远跳过
        return List.of("enigmaticlegacy");
    }
}

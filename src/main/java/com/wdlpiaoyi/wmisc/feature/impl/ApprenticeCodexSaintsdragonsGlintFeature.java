package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.compat.foil.ItemRendererOverrides;
import com.wdlpiaoyi.wmisc.feature.FeatureRegistry;
import com.wdlpiaoyi.wmisc.feature.WmiscFeature;

import java.util.List;
import java.util.Map;

/**
 * Apprentice's Codex × Saint's Dragons：把 Codex 的绑定武器自定义附魔光合并进本模组接管的
 * {@code getFoilBuffer} / {@code getFoilBufferDirect} 重定向里。
 *
 * <p>Codex 原本的 {@code @Redirect}（priority 900）被圣龙残影（1000）压着，一直静默失效；
 * 现在由 {@code ItemRendererAfterimageRedirectMixin} 统一持有那两条指令，本模块的开关决定
 * 是否在合并逻辑里加回 Codex 的 glint 缓冲。它依赖 {@code carbon_saintsdragons_foil} 在位
 * （那条模块负责在类加载期接管这些指令）。</p>
 */
public final class ApprenticeCodexSaintsdragonsGlintFeature implements WmiscFeature {

    @Override
    public String id() {
        return ItemRendererOverrides.MODULE_GLINT;
    }

    @Override
    public String summary() {
        return "Apprentice's Codex × Saint's Dragons：在合并后的 getFoilBuffer*/hasFoil 重定向里"
                + "恢复绑定武器自定义附魔光，与血暴残影染色同时生效";
    }

    @Override
    public List<String> requiredMods() {
        return List.of("apprenticecodex", "saintsdragons");
    }

    @Override
    public void onCommonSetup() {
        ItemRendererOverrides.setGlintModule(true);
    }

    @Override
    public void onConfigReload(Map<String, String> values) {
        ItemRendererOverrides.setGlintModule(FeatureRegistry.liveActive(this.id()));
    }
}
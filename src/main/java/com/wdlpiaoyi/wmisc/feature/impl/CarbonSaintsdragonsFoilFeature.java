package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.compat.foil.ItemRendererOverrides;
import com.wdlpiaoyi.wmisc.feature.WmiscFeature;

import java.util.List;

/**
 * carbon × Saint's Dragons：{@code ItemRenderer#render} 上 {@code hasFoil} 的 {@code @Redirect} 冲突。
 *
 * <p>carbon 的 {@code imo$foil} 是 {@code require = 1}，圣龙那条同优先级先到就先占，carbon 被跳过
 * → {@code InjectionError: (0/1) succeeded}，整合包直接起不来（崩在 mod 构造之前）。所以本模块
 * <b>不抢</b>那条指令：把圣龙的 {@code client.BloodTempestAfterimageItemRendererMixin} 从
 * {@code saintsdragons.mixins.json} 里摘掉（{@code WmiscMixinPlugin#acceptTargets}），
 * {@code hasFoil} 留给 carbon 独占，圣龙的其余三条注入由 {@code ItemRendererAfterimageRedirectMixin}
 * 复刻（含残影期间的附魔光抑制）。</p>
 *
 * <p>加载期动作，配置开关只能启动前改（运行期热改无效）。</p>
 */
public final class CarbonSaintsdragonsFoilFeature implements WmiscFeature {

    @Override
    public String id() {
        return ItemRendererOverrides.MODULE_FOIL;
    }

    @Override
    public String summary() {
        return "carbon × Saint's Dragons：把 hasFoil 的 @Redirect 让给 carbon 独占（它 require=1，被抢会崩档），"
                + "摘掉圣龙那条 ItemRenderer mixin 并复刻其其余注入，血暴残影与掉落物阴影捕获都不画附魔光";
    }

    @Override
    public List<String> requiredMods() {
        return List.of("carbon", "saintsdragons");
    }
}
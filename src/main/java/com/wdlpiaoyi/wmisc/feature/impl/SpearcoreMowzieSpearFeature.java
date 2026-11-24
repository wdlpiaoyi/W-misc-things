package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.compat.Compat;
import com.wdlpiaoyi.wmisc.compat.spearcore.MowzieSpearCompat;
import com.wdlpiaoyi.wmisc.feature.FeatureRegistry;
import com.wdlpiaoyi.wmisc.feature.WmiscFeature;
import java.util.List;
import java.util.Map;

public final class SpearcoreMowzieSpearFeature implements WmiscFeature {
    public String id() {
        return "spearcore_mowzie_spear";
    }

    public String summary() {
        return "让 mowziesmobs:spear 走 SpearCore 的长矛逻辑：左键戳刺、右键蓄力、挥刺/蓄力动画，攻击力属性照抄木矛；保留原 +1.5 格距离与 1/3 秒杀小动物";
    }

    public List<String> requiredMods() {
        return List.of("mowziesmobs", "spearcore");
    }

    public void onConstruct() {
        if (Compat.isModLoaded("mowziesmobs") && Compat.isModLoaded("spearcore")) {
            MowzieSpearCompat.install();
        }

    }

    public void onCommonSetup() {
        MowzieSpearCompat.setEnabled(true);
    }

    public void onConfigReload(Map<String, String> values) {
        MowzieSpearCompat.setEnabled(FeatureRegistry.liveActive(this.id()));
    }
}

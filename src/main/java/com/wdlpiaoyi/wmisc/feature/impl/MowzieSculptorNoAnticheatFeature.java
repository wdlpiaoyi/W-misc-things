package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.compat.mowzie.SculptorTestCompat;
import com.wdlpiaoyi.wmisc.feature.FeatureRegistry;
import com.wdlpiaoyi.wmisc.feature.WmiscFeature;
import java.util.List;
import java.util.Map;

public final class MowzieSculptorNoAnticheatFeature implements WmiscFeature {
    public String id() {
        return "mowzie_sculptor_no_anticheat";
    }

    public String summary() {
        return "去掉雕塑家试炼的反作弊：放/破方块、传送、飞行、游泳、向上加速、离开场地都不再判失败，场地里有方块也能开始";
    }

    public List<String> requiredMods() {
        return List.of("mowziesmobs");
    }

    public void onCommonSetup() {
        SculptorTestCompat.setActive(true);
    }

    public void onConfigReload(Map<String, String> values) {
        SculptorTestCompat.setActive(FeatureRegistry.liveActive(this.id()));
    }
}

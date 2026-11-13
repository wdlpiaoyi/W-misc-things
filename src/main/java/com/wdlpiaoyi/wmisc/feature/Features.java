package com.wdlpiaoyi.wmisc.feature;

import com.wdlpiaoyi.wmisc.feature.impl.ApollyonCurioReturnFeature;
import com.wdlpiaoyi.wmisc.feature.impl.ChlorideJeiFeature;
import com.wdlpiaoyi.wmisc.feature.impl.ContentFilterFeature;
import com.wdlpiaoyi.wmisc.feature.impl.DeadlockFixFeature;
import com.wdlpiaoyi.wmisc.feature.impl.EnigmaticCreativeFeature;
import com.wdlpiaoyi.wmisc.feature.impl.FantasyUpgradeOrbFeature;

/**
 * 全部功能模块的清单。顺序决定配置文件和启动日志里的顺序。
 */
public final class Features {
    private static boolean initialized;

    private Features() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        FeatureRegistry.register(new DeadlockFixFeature());
        FeatureRegistry.register(new FantasyUpgradeOrbFeature());
        FeatureRegistry.register(new ChlorideJeiFeature());
        FeatureRegistry.register(new ApollyonCurioReturnFeature());
        FeatureRegistry.register(new EnigmaticCreativeFeature());
        FeatureRegistry.register(new ContentFilterFeature());
    }
}

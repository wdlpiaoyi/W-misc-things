package com.wdlpiaoyi.wmisc.feature;

import com.wdlpiaoyi.wmisc.feature.impl.ApollyonCurioReturnFeature;
import com.wdlpiaoyi.wmisc.feature.impl.ApprenticeCodexSaintsdragonsGlintFeature;
import com.wdlpiaoyi.wmisc.feature.impl.CarbonSaintsdragonsFoilFeature;
import com.wdlpiaoyi.wmisc.feature.impl.ChlorideJeiFeature;
import com.wdlpiaoyi.wmisc.feature.impl.ContentFilterFeature;
import com.wdlpiaoyi.wmisc.feature.impl.DeadlockFixFeature;
import com.wdlpiaoyi.wmisc.feature.impl.EnigmaticCreativeFeature;
import com.wdlpiaoyi.wmisc.feature.impl.EnigmaticSoulRecoveryFeature;
import com.wdlpiaoyi.wmisc.feature.impl.FantasyUpgradeOrbFeature;
import com.wdlpiaoyi.wmisc.feature.impl.GoeticGgtuDimensionFeature;
import com.wdlpiaoyi.wmisc.feature.impl.IronsArcaneAnvilJeiFeature;
import com.wdlpiaoyi.wmisc.feature.impl.L2ModularBlockFinalFeature;
import com.wdlpiaoyi.wmisc.feature.impl.MowzieSculptorNoAnticheatFeature;
import com.wdlpiaoyi.wmisc.feature.impl.NoNarratorFeature;
import com.wdlpiaoyi.wmisc.feature.impl.SpearcoreMowzieSpearFeature;
import com.wdlpiaoyi.wmisc.feature.impl.WtcResourceHudFeature;

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
        FeatureRegistry.register(new L2ModularBlockFinalFeature());
        FeatureRegistry.register(new FantasyUpgradeOrbFeature());
        FeatureRegistry.register(new ChlorideJeiFeature());
        FeatureRegistry.register(new IronsArcaneAnvilJeiFeature());
        FeatureRegistry.register(new ApollyonCurioReturnFeature());
        FeatureRegistry.register(new EnigmaticCreativeFeature());
        FeatureRegistry.register(new EnigmaticSoulRecoveryFeature());
        FeatureRegistry.register(new GoeticGgtuDimensionFeature());
        FeatureRegistry.register(new ContentFilterFeature());
        FeatureRegistry.register(new SpearcoreMowzieSpearFeature());
        FeatureRegistry.register(new MowzieSculptorNoAnticheatFeature());
        FeatureRegistry.register(new WtcResourceHudFeature());
        FeatureRegistry.register(new NoNarratorFeature());
        FeatureRegistry.register(new CarbonSaintsdragonsFoilFeature());
        FeatureRegistry.register(new ApprenticeCodexSaintsdragonsGlintFeature());
    }
}

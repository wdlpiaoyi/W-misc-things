package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.feature.FeatureRegistry;
import com.wdlpiaoyi.wmisc.feature.WmiscFeature;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * 创造模式下无视 enigmatic legacy 七咒之戒的佩戴时长条件。
 *
 * <p>「佩戴时长」不是分钟数，而是一个比例：
 * {@code timeWithCurses / (timeWithCurses + timeWithoutCurses) >= CursedRingSuperCursedTime}
 * （默认 0.995），服务端每 tick +1，存在玩家的 {@code enigmaticlegacy:playtime_counter} 能力里。
 * 判定唯一入口是 {@code SuperpositionHandler#isTheWorthyOne(Player)}，只拦「值得之人限定物」。</p>
 *
 * <p>实现：{@code MixinSuperpositionHandlerWorthy} 在该方法 RETURN 处注入，创造模式玩家直接改成 true。
 * 与附属 EnigmaticAddons 在同一方法同一注入点的 RETURN 注入兼容（两边都只会把 false 改 true）。</p>
 *
 * <p>不伪造计数：附属的救赎之戒等级、{@code /getringtime}、tooltip 百分比都保持真实。</p>
 */
public final class EnigmaticCreativeFeature implements WmiscFeature {
    private static EnigmaticCreativeFeature instance;
    private static ForgeConfigSpec.BooleanValue creativeOnly;

    public EnigmaticCreativeFeature() {
        instance = this;
    }

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
        return true;
    }

    @Override
    public List<String> requiredMods() {
        return List.of("enigmaticlegacy");
    }

    @Override
    public void defineOptions(ForgeConfigSpec.Builder builder) {
        creativeOnly = builder.comment("只对创造模式生效；关掉则所有模式都跳过时长判定")
                .define("creative_only", true);
    }

    /** 供 mixin 调用：该玩家是否应当被当作「值得之人」。 */
    public static boolean shouldBypassWorthyGate(Player player) {
        if (instance == null || player == null || !FeatureRegistry.isActive(instance)) {
            return false;
        }
        boolean onlyCreative;
        try {
            onlyCreative = creativeOnly == null || creativeOnly.get();
        } catch (IllegalStateException configNotLoaded) {
            onlyCreative = true;
        }
        return !onlyCreative || player.isCreative();
    }
}

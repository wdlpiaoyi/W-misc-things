package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.feature.WmiscFeature;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * goety_revelation 的 Apollyon：最后阶段会没收玩家饰品，打完把饰品装进一个水晶发回来，
 * 但它是直接往物品栏塞，饰品一多就溢出到地上。改成用水晶一键装回原槽位。
 *
 * <p>难点：部分饰品本身提供额外槽位，卸下后对应槽位就不存在了，所以装回的顺序有讲究
 * （先装提供槽位的饰品，再装依赖这些槽位的）。</p>
 *
 * <p>TODO：确认 goety_revelation 的 modid、水晶物品与「没收 / 归还」的入口方法。</p>
 */
public final class ApollyonCurioReturnFeature implements WmiscFeature {
    @Override
    public String id() {
        return "goety_apollyon_curio_return";
    }

    @Override
    public String summary() {
        return "Apollyon 战后的饰品按原槽位装回，而不是塞物品栏";
    }

    @Override
    public boolean implemented() {
        return false;
    }

    @Override
    public List<String> requiredMods() {
        return List.of("goety_revelation", "curios");
    }

    @Override
    public void defineOptions(ForgeConfigSpec.Builder builder) {
        builder.comment("优先还原到没收前的槽位；否则退回按槽位类型贪心填充")
                .define("prefer_original_slots", true);
        builder.comment("槽位实在装不下的部分掉到玩家脚下，而不是强塞物品栏")
                .define("drop_overflow_to_ground", true);
    }
}

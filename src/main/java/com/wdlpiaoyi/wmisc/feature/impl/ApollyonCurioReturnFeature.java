package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.compat.apollyon.ApollyonCrystalHandler;
import com.wdlpiaoyi.wmisc.feature.WmiscFeature;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;

/**
 * GoetyRevelation 的 Apollyon 战后归还：水晶不再把饰品一件件掉到脚下，而是按原槽位装回。
 *
 * <p>流程：RevelationFix 在 Apollyon 血量 ≤7.14% 时没收 4 格盔甲 + 全部 curios，只存 ItemStack NBT
 * （没有槽位），战后在尸体箱子发一块「饰品储存水晶」。原逻辑是右键吐一件、Shift 右键全吐到地上。</p>
 *
 * <p>本模块：没收前（{@code processInventory} HEAD）记录槽位快照 → 存档在玩家 ForgeData；
 * 右键水晶时拦截 {@code RightClickBlock}，按快照装回原位；槽位提供者先装，装完手动应用
 * slot modifier，保证同一 tick 内就能用上扩出来的槽位。</p>
 */
public final class ApollyonCurioReturnFeature implements WmiscFeature {
    private static final String SECTION = "features.goety_apollyon_curio_return";

    private static ForgeConfigSpec.BooleanValue preferOriginalSlots;
    private static ForgeConfigSpec.BooleanValue dropOverflowToGround;

    private static volatile boolean active;

    @Override
    public String id() {
        return "goety_apollyon_curio_return";
    }

    @Override
    public String summary() {
        return "Apollyon 战后水晶把饰品按原槽位装回，而不是一件件掉到脚下";
    }

    @Override
    public boolean implemented() {
        return true;
    }

    @Override
    public List<String> requiredMods() {
        return List.of("revelationfix", "curios");
    }

    @Override
    public void defineOptions(ForgeConfigSpec.Builder builder) {
        preferOriginalSlots = builder.comment("优先按没收前的槽位快照还原；关掉则按槽位类型贪心填充")
                .define("prefer_original_slots", true);
        dropOverflowToGround = builder.comment("实在装不下的部分直接掉到脚下（关掉则先塞物品栏）")
                .define("drop_overflow_to_ground", true);
    }

    @Override
    public void onCommonSetup() {
        active = true;
        MinecraftForge.EVENT_BUS.addListener(ApollyonCrystalHandler::onRightClickBlock);
        Wmisc.LOG.info("[w_misc] Apollyon 饰品归还原槽位已启用");
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean preferOriginalSlots() {
        try {
            return preferOriginalSlots == null || preferOriginalSlots.get();
        } catch (IllegalStateException configNotLoaded) {
            return true;
        }
    }

    public static boolean dropOverflowToGround() {
        try {
            return dropOverflowToGround == null || dropOverflowToGround.get();
        } catch (IllegalStateException configNotLoaded) {
            return true;
        }
    }

    /** 配置段名，供日志/工具引用。 */
    public static String section() {
        return SECTION;
    }
}

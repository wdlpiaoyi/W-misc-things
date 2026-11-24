package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.feature.FeatureRegistry;
import com.wdlpiaoyi.wmisc.feature.WmiscFeature;
import com.wdlpiaoyi.wmisc.util.ConfigPeek;
import java.util.List;
import java.util.Map;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;

public final class IronsArcaneAnvilJeiFeature implements WmiscFeature {
    private static final String SECTION = "features.irons_arcane_anvil_jei";
    private static final String TESTED_VERSION = "1.20.1-3.16.3";
    private static final int DEFAULT_MAX_ITEMS_PER_SLOT = 16;
    private static final int DEFAULT_MAX_IMBUE_RECIPES = 12;
    private static final int DEFAULT_MAX_UPGRADE_PER_ORB = 8;
    private static ForgeConfigSpec.IntValue maxItemsPerSlot;
    private static ForgeConfigSpec.IntValue maxImbueRecipes;
    private static ForgeConfigSpec.IntValue maxUpgradeRecipesPerOrb;
    private static volatile boolean active;

    public String id() {
        return "irons_arcane_anvil_jei";
    }

    public String summary() {
        return "裁掉奥术铁砧 JEI 配方的组合爆炸（配方条数与槽位候选设上限）";
    }

    public List<String> requiredMods() {
        return List.of("irons_spellbooks", "jei");
    }

    public void defineOptions(ForgeConfigSpec.Builder builder) {
        maxItemsPerSlot = builder.comment(new String[]{"每条配方每个槽位最多保留多少个候选物品（均匀取样，0 = 不裁剪）", "灌注类配方原版会把「全部法术 × 全部等级」的卷轴塞进同一个槽位，这是注册期内存的大头"}).defineInRange("max_items_per_slot", 16, 0, 256);
        maxImbueRecipes = builder.comment("灌注类配方最多留几条（原版是整合包里每一把剑一条；均匀取样，0 = 不限制）").defineInRange("max_imbue_recipes", 12, 0, 4096);
        maxUpgradeRecipesPerOrb = builder.comment("每种升级宝珠最多留几条升级配方（原版是整合包里每一件盔甲一条；均匀取样，0 = 不限制）").defineInRange("max_upgrade_recipes_per_orb", 8, 0, 4096);
    }

    public void onClientSetup() {
        active = true;
        String version = ironsVersion();
        if (!"1.20.1-3.16.3".equals(version)) {
            Wmisc.LOG.warn("[w_misc] 奥术铁砧 JEI 瘦身是按 irons_spellbooks {} 写的，当前是 {}；配方结构对不上时注入会自动跳过（require = 0），关掉本模块即可恢复原样", "1.20.1-3.16.3", version);
        }

        Wmisc.LOG.info("[w_misc] 奥术铁砧 JEI 瘦身已启用：每槽 ≤ {}、灌注配方 ≤ {}、每宝珠升级配方 ≤ {}（0 表示不限制）", new Object[]{maxItemsPerSlot(), maxImbueRecipes(), maxUpgradeRecipesPerOrb()});
    }

    public void onConfigReload(Map<String, String> values) {
        ConfigPeek.applyInt(values, "max_items_per_slot", maxItemsPerSlot);
        ConfigPeek.applyInt(values, "max_imbue_recipes", maxImbueRecipes);
        ConfigPeek.applyInt(values, "max_upgrade_recipes_per_orb", maxUpgradeRecipesPerOrb);
    }

    public static boolean isActive() {
        return active && FeatureRegistry.liveActive("irons_arcane_anvil_jei");
    }

    public static int maxItemsPerSlot() {
        return read(maxItemsPerSlot, 16);
    }

    public static int maxImbueRecipes() {
        return read(maxImbueRecipes, 12);
    }

    public static int maxUpgradeRecipesPerOrb() {
        return read(maxUpgradeRecipesPerOrb, 8);
    }

    public static String section() {
        return "features.irons_arcane_anvil_jei";
    }

    private static int read(ForgeConfigSpec.IntValue value, int fallback) {
        if (value == null) {
            return fallback;
        } else {
            try {
                return (Integer)value.get();
            } catch (IllegalStateException var3) {
                return fallback;
            }
        }
    }

    private static String ironsVersion() {
        try {
            return (String)ModList.get().getModContainerById("irons_spellbooks").map(ModContainer::getModInfo).map((info) -> info.getVersion().toString()).orElse("(未知)");
        } catch (Throwable var1) {
            return "(未知)";
        }
    }
}

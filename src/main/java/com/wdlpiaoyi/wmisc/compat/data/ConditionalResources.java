package com.wdlpiaoyi.wmisc.compat.data;

import com.wdlpiaoyi.wmisc.feature.FeatureRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * 随包携带、但只有前置齐全时才成立的数据文件。
 *
 * <p>数据包是按命名空间合并加载的，跟「目标模组在不在 mod 列表里」完全无关：只要本模组在，
 * {@code data/fantasy_ending/...} 就会进数据包。于是 fantasy_ending 没装时，
 * 这份 {@code upgrade_orb_type} 引用的属性 / 物品都不存在，irons_spellbooks 加载数据包注册表时
 * 会直接报 {@code Failed to parse fantasy_ending:irons_spellbooks/upgrade_orb_type/fantasy_power.json}
 * 并让整个注册表加载失败（进世界时崩）。</p>
 *
 * <p>所以在 {@code MultiPackResourceManager} 这一层把它藏起来：模块未启用（开关关闭 / 前置缺失）
 * 时，上层根本看不到这份文件。</p>
 */
public final class ConditionalResources {
    private static final ResourceLocation FANTASY_POWER_ORB =
            new ResourceLocation("fantasy_ending", "irons_spellbooks/upgrade_orb_type/fantasy_power.json");

    private static final String FANTASY_MODULE = "fantasy_ending_upgrade_orb";

    private ConditionalResources() {
    }

    /** 这份资源在当前实例里是否应该被隐藏。 */
    public static boolean isHidden(ResourceLocation id) {
        if (FANTASY_POWER_ORB.equals(id)) {
            // liveActive = 配置开关打开 且 前置齐全；两者任一不满足时这份条目不成立。
            return !FeatureRegistry.liveActive(FANTASY_MODULE);
        }
        return false;
    }
}

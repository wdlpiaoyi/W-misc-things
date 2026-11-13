package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.compat.spellbooks.UpgradeOrbEntryCheck;
import com.wdlpiaoyi.wmisc.feature.WmiscFeature;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;

/**
 * 补一条 irons_spellbooks 的 upgrade_orb_type 数据条目。
 *
 * <p>irons_spellbooks 3.16 起把 {@code irons_spellbooks:upgrade_orb_type} 改成了 datapack
 * registry（{@code data/<ns>/irons_spellbooks/upgrade_orb_type/<name>.json}）；fantasy_ending
 * 2.7.20 仍按旧机制在代码里注册，硬编码资源键 {@code fantasy_ending:fantasy_power}，
 * 却没有随包提供数据文件 → JEI/EMI 报 240 条 Missing element，奥术铁砧配方全部失效。</p>
 *
 * <p>模块内容是纯资源：随 jar 提供
 * {@code data/fantasy_ending/irons_spellbooks/upgrade_orb_type/fantasy_power.json}
 * （amount=0.05、attribute=fantasy_ending:fantasy_spell_power、operation=MULTIPLY_BASE，
 * 与原代码里的注册值一致）。开关只控制启动时的校验日志。</p>
 */
public final class FantasyUpgradeOrbFeature implements WmiscFeature {
    @Override
    public String id() {
        return "fantasy_ending_upgrade_orb";
    }

    @Override
    public String summary() {
        return "补 fantasy_ending:fantasy_power 的 upgrade_orb_type 数据条目";
    }

    @Override
    public boolean implemented() {
        return true;
    }

    @Override
    public List<String> requiredMods() {
        return List.of("irons_spellbooks", "fantasy_ending");
    }

    @Override
    public void onCommonSetup() {
        boolean shipped = FantasyUpgradeOrbFeature.class.getResource(
                "/data/fantasy_ending/irons_spellbooks/upgrade_orb_type/fantasy_power.json") != null;
        if (!shipped) {
            Wmisc.LOG.error("[w_misc] 本模组缺少 fantasy_power.json，fantasy_ending 的升级宝珠会失效");
        } else {
            Wmisc.LOG.info("[w_misc] 已随包提供 fantasy_ending:fantasy_power 的 upgrade_orb_type 条目");
        }
        MinecraftForge.EVENT_BUS.addListener(UpgradeOrbEntryCheck::onServerStarted);
    }
}

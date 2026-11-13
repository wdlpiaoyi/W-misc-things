package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.feature.WmiscFeature;

import java.util.List;

/**
 * 补一条 irons_spellbooks 的 upgrade_orb_type 数据条目。
 *
 * <p>irons_spellbooks 3.16 起把 {@code irons_spellbooks:upgrade_orb_type} 改成了 datapack
 * registry（{@code data/<ns>/irons_spellbooks/upgrade_orb_type/<name>.json}）；fantasy_ending
 * 2.7.20 仍按旧机制在代码里注册，硬编码资源键 {@code fantasy_ending:fantasy_power}，
 * 却没有随包提供数据文件 → 查询时抛 Missing element。</p>
 *
 * <p>模块内容是纯资源：随 jar 提供
 * {@code data/fantasy_ending/irons_spellbooks/upgrade_orb_type/fantasy_power.json}。
 * 开关只用于启动时的校验日志（条目缺失就点名报出来）。</p>
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
        // TODO: 补 datapack json，并在 common setup 里校验注册表里能查到该键
        return false;
    }

    @Override
    public List<String> requiredMods() {
        return List.of("irons_spellbooks", "fantasy_ending");
    }
}

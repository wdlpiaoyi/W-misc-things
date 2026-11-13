package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.feature.WmiscFeature;

/**
 * 把 chloride-1.8.1-jei15.70-compat 那个工作区的兼容改动并进本模组。
 *
 * <p>目标是让实例里不再需要那个独立 jar：本模组装上后替代它。具体改哪些类、依赖哪些
 * 前置，等把那个工作区的源码搬进来时再确定（TODO：确认 chloride / jei 的 modid 与版本范围，
 * 然后补 requiredMods）。</p>
 */
public final class ChlorideJeiFeature implements WmiscFeature {
    @Override
    public String id() {
        return "chloride_jei_bundle";
    }

    @Override
    public String summary() {
        return "并入 chloride-1.8.1 / JEI 15.70 兼容改动，替代独立兼容 jar";
    }

    @Override
    public boolean implemented() {
        return false;
    }
}

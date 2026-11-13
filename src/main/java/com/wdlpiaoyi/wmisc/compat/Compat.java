package com.wdlpiaoyi.wmisc.compat;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.LoadingModList;

/**
 * 跨模组探测工具。
 *
 * <p>极早期（Mixin 插件阶段）Forge 还没构建好 {@link ModList}，只能退到
 * {@link LoadingModList}；两者都不可用时按「不存在」处理，让模块跳过而不是崩。</p>
 */
public final class Compat {
    private Compat() {
    }

    public static boolean isModLoaded(String modId) {
        try {
            ModList modList = ModList.get();
            if (modList != null) {
                return modList.isLoaded(modId);
            }
        } catch (Throwable ignored) {
            // ModList 尚未就绪，继续往下试
        }

        try {
            LoadingModList loading = LoadingModList.get();
            return loading != null && loading.getModFileById(modId) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }
}

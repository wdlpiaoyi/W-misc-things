package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.feature.WmiscFeature;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public final class L2ModularBlockFinalFeature implements WmiscFeature {
    public static final String SECTION = "features.l2modularblock_final_fix";
    private static final String IMPL_CLASS = "dev.xkmc.l2modularblock.DelegateBlockImpl";

    public String id() {
        return "l2modularblock_final_fix";
    }

    public String summary() {
        return "摘掉 l2modularblock 新版加上的 final 标记，救回照旧版编译的附属（如 arsdelight）";
    }

    public boolean implemented() {
        return true;
    }

    public void onCommonSetup() {
        List<String> stillFinal = stillFinalMethods();
        if (stillFinal == null) {
            Wmisc.LOG.debug("[w_misc] 没找到 {}，跳过 l2modularblock 自检", "dev.xkmc.l2modularblock.DelegateBlockImpl");
        } else {
            if (stillFinal.isEmpty()) {
                Wmisc.LOG.info("[w_misc] l2modularblock final 修补已生效：{} 里已没有可被覆写的 final 方法", "dev.xkmc.l2modularblock.DelegateBlockImpl");
            } else {
                Wmisc.LOG.error("[w_misc] l2modularblock final 修补未生效：{} 里仍有 {} 个 final 方法 {}；arsdelight 等老版附属会在 CONSTRUCT 阶段失败，请检查 Mixin 是否被别的模组抢先变换", new Object[]{"dev.xkmc.l2modularblock.DelegateBlockImpl", stillFinal.size(), stillFinal});
            }

        }
    }

    private static List<String> stillFinalMethods() {
        try {
            Class<?> impl = Class.forName("dev.xkmc.l2modularblock.DelegateBlockImpl", false, L2ModularBlockFinalFeature.class.getClassLoader());
            List<String> names = new ArrayList();

            for(Method method : impl.getDeclaredMethods()) {
                if (!method.getName().startsWith("<")) {
                    int modifiers = method.getModifiers();
                    if (Modifier.isFinal(modifiers) && !Modifier.isPrivate(modifiers) && !Modifier.isStatic(modifiers)) {
                        names.add(method.getName());
                    }
                }
            }

            return names;
        } catch (LinkageError | ClassNotFoundException var7) {
            return null;
        }
    }
}

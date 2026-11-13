package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.feature.WmiscFeature;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * 消除 iceandfire 与 l2weaponry 的 JVM 类初始化死锁（启动卡死在 construct 阶段，无报错、
 * 无输出，jstack 里两个线程都是 RUNNABLE）。
 *
 * <p>锁环：iceandfire 的构造线程持有 {@code IafBlocks} 的类初始化锁等待 {@code IafItems}；
 * l2weaponry 的构造线程持有 {@code IafItems} 的类初始化锁等待 {@code IafBlocks}。</p>
 *
 * <p>做法：只给这两个模组的 {@code FMLModContainer#constructMod} 加一把共享锁，让它们不并发
 * 构造，其余模组照常并行。详细论证见本地笔记（已 gitignore，不入库）。</p>
 */
public final class DeadlockFixFeature implements WmiscFeature {
    @Override
    public String id() {
        return "deadlock_fix";
    }

    @Override
    public String summary() {
        return "串行化 iceandfire 与 l2weaponry 的 constructMod，消除类初始化锁环";
    }

    @Override
    public boolean implemented() {
        // TODO: 注入 FMLModContainer#constructMod（HEAD 加锁 / RETURN 放锁），见 mixin 包
        return false;
    }

    @Override
    public List<String> requiredMods() {
        return List.of("iceandfire", "l2weaponry");
    }

    @Override
    public void defineOptions(ForgeConfigSpec.Builder builder) {
        builder.comment("锁只覆盖 iceandfire / l2weaponry 这一对；出问题时切成全局串行（启动变慢但确定）")
                .define("global_serialization_fallback", false);
        builder.comment("打印每次加锁 / 放锁的日志，用于确认注入生效")
                .define("log_lock_events", false);
    }
}

package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.compat.deadlock.DeadlockGuard;
import com.wdlpiaoyi.wmisc.feature.WmiscFeature;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Set;

/**
 * 消除 iceandfire 与 l2weaponry 的 JVM 类初始化死锁（启动卡死在 construct 阶段，无报错、无输出，
 * jstack 里两个线程都是 RUNNABLE）。
 *
 * <p>锁环：iceandfire 的构造线程持有 {@code IafBlocks} 的类初始化锁等待 {@code IafItems}；
 * l2weaponry 的构造线程持有 {@code IafItems} 的类初始化锁等待 {@code IafBlocks}。</p>
 *
 * <p>实现：{@code IceAndFireConstructMixin} / {@code L2WeaponryConstructMixin} 分别注入两个 mod
 * 主类的构造函数 HEAD/RETURN，共用一把锁，保证两者不同时进入构造阶段；其余模组照常并行。
 * 不用 {@code FMLModContainer} 作为注入点：它在 ModLauncher 启动层，Mixin 覆盖不到（已实测）。</p>
 */
public final class DeadlockFixFeature implements WmiscFeature {
    @Override
    public String id() {
        return "deadlock_fix";
    }

    @Override
    public String summary() {
        return "串行化 iceandfire 与 l2weaponry 的构造阶段，消除类初始化锁环";
    }

    @Override
    public boolean implemented() {
        return true;
    }

    @Override
    public List<String> requiredMods() {
        return List.of("iceandfire", "l2weaponry");
    }

    @Override
    public void defineOptions(ForgeConfigSpec.Builder builder) {
        builder.comment("打印每次加锁 / 放锁的日志（默认只在启动时各打一行）")
                .define("log_lock_events", false);
    }

    @Override
    public void onCommonSetup() {
        Set<String> engaged = DeadlockGuard.engaged();
        if (engaged.containsAll(DeadlockGuard.pair())) {
            Wmisc.LOG.info("[w_misc] 死锁修复已生效：iceandfire 与 l2weaponry 的构造阶段已串行化");
        } else {
            Wmisc.LOG.warn("[w_misc] 死锁修复未生效：只接管了 {}（期望 {}）。"
                            + "可能这两个模组的构造入口变了，需要复核 mixin 目标类",
                    engaged.isEmpty() ? "无" : String.join(", ", engaged), String.join(" + ", DeadlockGuard.pair()));
        }
    }
}

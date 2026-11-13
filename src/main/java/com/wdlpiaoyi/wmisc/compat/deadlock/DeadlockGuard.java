package com.wdlpiaoyi.wmisc.compat.deadlock;

import com.wdlpiaoyi.wmisc.util.ConfigPeek;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * iceandfire × l2weaponry 构造期串行化。
 *
 * <p>问题：Forge 把各 mod 的构造提交到 ForkJoinPool 并行执行。iceandfire 的构造线程持有
 * {@code IafBlocks} 的类初始化锁并等待 {@code IafItems}，l2weaponry 的构造线程正好相反，
 * 构成 JVM 类初始化锁环 —— 两个线程都是 RUNNABLE，进程假死、日志停写、jstack 不报死锁。</p>
 *
 * <p>做法：在这两个 mod 各自主类的构造函数 HEAD 处取同一把共享 {@link ReentrantLock}、RETURN 处释放，
 * 保证两者不同时进入构造阶段；被挡住的线程还没碰过任何一个 Iaf* 类，因此不可能再成环。
 * 其余 mod 照旧并行。</p>
 *
 * <p>为什么不用 Mixin 注入 {@code FMLModContainer#constructMod}：那个类在 ModLauncher 的启动层，
 * Mixin 只处理游戏层（mod）类，注入不会被应用。改注入 mod 主类是等效的，且确实落在 Mixin 的
 * 应用范围内。</p>
 *
 * <p>开关在「Forge 配置加载之前」就要生效，所以不读 ForgeConfigSpec，而是用 {@link ConfigPeek}
 * 直接读 {@code [features.deadlock_fix]}；系统属性可覆盖，供排查用。</p>
 */
public final class DeadlockGuard {
    private static final Logger LOG = LoggerFactory.getLogger("w_misc/deadlock_fix");

    private static final String SECTION = "features.deadlock_fix";
    private static final String PROP_ENABLED = "w_misc.deadlock_fix.enabled";
    private static final String PROP_LOG = "w_misc.deadlock_fix.log";

    public static final String ICEANDFIRE = "iceandfire";
    public static final String L2WEAPONRY = "l2weaponry";

    /** 这对 mod 共用一把锁：不同时进入构造阶段。 */
    private static final ReentrantLock PAIR_LOCK = new ReentrantLock();

    private static final Set<String> ENGAGED = ConcurrentHashMap.newKeySet();

    private static final boolean ENABLED = resolveEnabled();
    private static final boolean LOG_EVENTS = resolveLogEvents();

    private DeadlockGuard() {
    }

    /**
     * 在 mod 主类构造函数 HEAD 处调用。返回要释放的锁，不需要串行化时返回 null。
     */
    public static ReentrantLock acquire(String modId) {
        if (!ENABLED || modId == null || !(ICEANDFIRE.equals(modId) || L2WEAPONRY.equals(modId))) {
            return null;
        }
        if (LOG_EVENTS) {
            LOG.info("[deadlock_fix] waiting ({}) thread={}", modId, Thread.currentThread().getName());
        }
        PAIR_LOCK.lock();
        if (ENGAGED.add(modId)) {
            LOG.info("[deadlock_fix] 已接管 {} 的构造阶段（构造串行化生效）", modId);
        }
        if (LOG_EVENTS) {
            LOG.info("[deadlock_fix] acquired ({}) thread={}", modId, Thread.currentThread().getName());
        }
        return PAIR_LOCK;
    }

    public static void release(ReentrantLock lock, String modId) {
        if (lock == null) {
            return;
        }
        lock.unlock();
        if (LOG_EVENTS) {
            LOG.info("[deadlock_fix] released ({}) thread={}", modId, Thread.currentThread().getName());
        }
    }

    /** 已经真正接管过构造阶段的 mod（用于启动自检：两个都在才说明修复确实生效）。 */
    public static Set<String> engaged() {
        return Set.copyOf(ENGAGED);
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    /** 需要串行化的模组对，供日志引用。 */
    public static List<String> pair() {
        return List.of(ICEANDFIRE, L2WEAPONRY);
    }

    private static boolean resolveEnabled() {
        String override = System.getProperty(PROP_ENABLED);
        if (override != null) {
            return Boolean.parseBoolean(override);
        }
        return ConfigPeek.getBoolean(SECTION, "enabled", true);
    }

    private static boolean resolveLogEvents() {
        String override = System.getProperty(PROP_LOG);
        if (override != null) {
            return Boolean.parseBoolean(override);
        }
        return ConfigPeek.getBoolean(SECTION, "log_lock_events", false);
    }
}

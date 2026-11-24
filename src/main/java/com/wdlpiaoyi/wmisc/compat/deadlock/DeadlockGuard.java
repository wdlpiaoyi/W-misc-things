package com.wdlpiaoyi.wmisc.compat.deadlock;

import com.wdlpiaoyi.wmisc.util.ConfigPeek;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * iceandfire × l2weaponry 初始化阶段的串行化。
 *
 * <p>问题：Forge 把各 mod 的构造提交到 ForkJoinPool 并行执行。iceandfire 的构造线程持有
 * {@code IafBlocks} 的类初始化锁并等待 {@code IafItems}，l2weaponry 的构造线程正好相反，
 * 构成 JVM 类初始化锁环 —— 两个线程都是 RUNNABLE，进程假死、日志停写、jstack 不报死锁。</p>
 *
 * <p>做法：在两个模组真正触碰 Iaf* 类的入口方法（{@code IceAndFire.init()} 与
 * {@code LWItems.register()}）外用一把共享 {@link ReentrantLock} 互斥；被挡住的线程还没碰过
 * 任何一个 Iaf* 类，因此不可能再成环。其余 mod 照旧并行。</p>
 *
 * <p>为什么不在构造函数里锁：Mixin 0.8.5 对构造函数只允许 TAIL 注入，
 * INVOKE 注入点会报 "Found @Inject targetting a constructor"。这两处入口方法覆盖了危险区。</p>
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

    /** 这对 mod 共用一把锁：不同时进入危险区。 */
    private static final ReentrantLock PAIR_LOCK = new ReentrantLock();

    /** 等锁超过这么久还没拿到，大概率另一侧卡死了 —— 打一条 warn 方便定位，但继续排队。 */
    private static final long WAIT_WARN_SECONDS = 60L;

    private static final Set<String> ENGAGED = ConcurrentHashMap.newKeySet();

    private static final boolean ENABLED = resolveEnabled();
    private static final boolean LOG_EVENTS = resolveLogEvents();

    private DeadlockGuard() {
    }

    /** 在入口方法 HEAD 处调用。 */
    public static void acquire(String modId) {
        if (!ENABLED || modId == null || !(ICEANDFIRE.equals(modId) || L2WEAPONRY.equals(modId))) {
            return;
        }
        if (LOG_EVENTS) {
            LOG.info("[deadlock_fix] waiting ({}) thread={}", modId, Thread.currentThread().getName());
        }
        awaitPairLock(modId);
        if (ENGAGED.add(modId)) {
            LOG.info("[deadlock_fix] 已接管 {} 的构造阶段（构造串行化生效）", modId);
        }
        if (LOG_EVENTS) {
            LOG.info("[deadlock_fix] acquired ({}) thread={}", modId, Thread.currentThread().getName());
        }
    }

    /**
     * 拿共享锁，但先限时等一次：等不到就打 warn 再无限排队。
     *
     * <p>被挡住的线程在入口方法里还没碰过任何一个 Iaf* 类，所以不存在成环的可能。</p>
     */
    private static void awaitPairLock(String modId) {
        try {
            if (PAIR_LOCK.tryLock(WAIT_WARN_SECONDS, TimeUnit.SECONDS)) {
                return;
            }
            LOG.warn("[deadlock_fix] {} 等共享锁超过 {} 秒，另一侧可能卡住了；继续排队。thread={}",
                    modId, WAIT_WARN_SECONDS, Thread.currentThread().getName());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        PAIR_LOCK.lock();
    }

    /** 在入口方法 RETURN 处调用；未持锁时是安全的 no-op。 */
    public static void release(String modId) {
        if (!ENABLED || !PAIR_LOCK.isHeldByCurrentThread()) {
            return;
        }
        PAIR_LOCK.unlock();
        if (LOG_EVENTS) {
            LOG.info("[deadlock_fix] released ({}) thread={}", modId, Thread.currentThread().getName());
        }
    }

    /** 已经真正接管过初始化阶段的 mod（用于启动自检：两个都在才说明修复确实生效）。 */
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

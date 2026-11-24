package com.wdlpiaoyi.wmisc.compat.spearcore;

import com.wdlpiaoyi.wmisc.Wmisc;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventListenerHelper;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;

public final class SpearCoreBridge {
    private static final ResourceLocation WOODEN_SPEAR_ID = new ResourceLocation("spearcore", "wooden_spear");
    private final Class<?> spearLikeType;
    private final Method tryStartCharging;
    private final Method tickCharge;
    private final Class<?> damageEventType;
    private final Object stabAttackType;
    private final Method damageAttackType;
    private final Method damageBaseDamage;
    private final Method damageMultiplierSetter;
    private final Method damageAttacker;
    private final Method damageTarget;
    private volatile Item woodenSpear;
    private volatile boolean woodenSpearMissingLogged;

    private SpearCoreBridge(Class<?> spearLikeType, Method tryStartCharging, Method tickCharge, Class<?> damageEventType, Object stabAttackType, Method damageAttackType, Method damageBaseDamage, Method damageMultiplierSetter, Method damageAttacker, Method damageTarget) {
        this.spearLikeType = spearLikeType;
        this.tryStartCharging = tryStartCharging;
        this.tickCharge = tickCharge;
        this.damageEventType = damageEventType;
        this.stabAttackType = stabAttackType;
        this.damageAttackType = damageAttackType;
        this.damageBaseDamage = damageBaseDamage;
        this.damageMultiplierSetter = damageMultiplierSetter;
        this.damageAttacker = damageAttacker;
        this.damageTarget = damageTarget;
    }

    public static SpearCoreBridge probe() {
        try {
            ClassLoader loader = SpearCoreBridge.class.getClassLoader();
            Class<?> spearLike = Class.forName("net.minecraft.spearcore.item.SpearLike", false, loader);
            Class<?> combat = Class.forName("net.minecraft.spearcore.init.SpearCombat", false, loader);
            Method start = combat.getMethod("tryStartCharging", Level.class, Player.class, InteractionHand.class, ItemStack.class, spearLike);
            Method tick = combat.getMethod("tickCharge", LivingEntity.class, ItemStack.class, spearLike);
            Class<?> damageEvent = null;
            Object stab = null;
            Method attackType = null;
            Method baseDamage = null;
            Method setMultiplier = null;
            Method attacker = null;
            Method target = null;

            try {
                damageEvent = Class.forName("net.minecraft.spearcore.event.SpearDamageEvent", false, loader);
                Class<?> attackTypeEnum = Class.forName("net.minecraft.spearcore.event.SpearDamageEvent$AttackType", false, loader);

                for(Object constant : attackTypeEnum.getEnumConstants()) {
                    if ("STAB".equals(((Enum)constant).name())) {
                        stab = constant;
                        break;
                    }
                }

                if (stab == null) {
                    throw new IllegalStateException("SpearDamageEvent.AttackType 里没有 STAB");
                }

                attackType = damageEvent.getMethod("getAttackType");
                baseDamage = damageEvent.getMethod("getBaseDamage");
                setMultiplier = damageEvent.getMethod("setMultiplier", Float.TYPE);
                attacker = damageEvent.getMethod("getAttacker");
                target = damageEvent.getMethod("getTarget");
            } catch (Throwable eventFailure) {
                Wmisc.LOG.warn("[w_misc] SpearCore 的 SpearDamageEvent 解析失败，Mowzie 长矛的秒杀小动物效果将不再生效", eventFailure);
                damageEvent = null;
                stab = null;
                attackType = null;
                baseDamage = null;
                setMultiplier = null;
                attacker = null;
                target = null;
            }

            return new SpearCoreBridge(spearLike, start, tick, damageEvent, stab, attackType, baseDamage, setMultiplier, attacker, target);
        } catch (Throwable t) {
            Wmisc.LOG.warn("[w_misc] SpearCore 反射桥建立失败，Mowzie 长矛兼容停用", t);
            return null;
        }
    }

    public Item woodenSpear() {
        Item cached = this.woodenSpear;
        if (cached != null) {
            return cached;
        } else {
            Item found = ForgeRegistries.ITEMS == null ? null : (Item)ForgeRegistries.ITEMS.getValue(WOODEN_SPEAR_ID);
            if (found != null && found != Items.AIR) {
                this.woodenSpear = found;
                return found;
            } else {
                if (!this.woodenSpearMissingLogged) {
                    this.woodenSpearMissingLogged = true;
                    Wmisc.LOG.warn("[w_misc] 注册表里找不到 spearcore:wooden_spear，Mowzie 长矛兼容等它出现后再生效");
                }

                return null;
            }
        }
    }

    public Object newSpearLike() {
        Object delegate = this.woodenSpear();
        return delegate == null ? null : Proxy.newProxyInstance(this.spearLikeType.getClassLoader(), new Class[]{this.spearLikeType}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(proxy, method, args);
            } else {
                try {
                    return method.invoke(delegate, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        });
    }

    private static Object handleObjectMethod(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        if ("toString".equals(name)) {
            return "MowzieSpearLike(spearcore:wooden_spear)";
        } else if ("hashCode".equals(name)) {
            return System.identityHashCode(proxy);
        } else {
            return !"equals".equals(name) ? null : proxy == (args != null && args.length == 1 ? args[0] : null);
        }
    }

    public InteractionResultHolder<ItemStack> tryStartCharging(Level level, Player player, InteractionHand hand, ItemStack stack, Object spearLike) {
        try {
            return (InteractionResultHolder)this.tryStartCharging.invoke((Object)null, level, player, hand, stack, spearLike);
        } catch (Throwable t) {
            Wmisc.LOG.warn("[w_misc] 调用 SpearCore 的 tryStartCharging 失败，本次右键按原版处理", t);
            return null;
        }
    }

    public void tickCharge(LivingEntity user, ItemStack stack, Object spearLike) {
        try {
            this.tickCharge.invoke((Object)null, user, stack, spearLike);
        } catch (Throwable t) {
            Wmisc.LOG.warn("[w_misc] 调用 SpearCore 的 tickCharge 失败，本次蓄力 tick 跳过", t);
        }

    }

    public boolean hasDamageEvent() {
        return this.damageEventType != null && this.stabAttackType != null;
    }

    public void listenSpearDamage(Consumer<Object> handler) {
        if (!this.hasDamageEvent()) {
            return;
        }
        try {
            try {
                seedListenerList(this.damageEventType);
            } catch (Throwable seedFailure) {
                // 预建失败也继续试注册：事件实例已经先查过表时，注册本身并不需要实例化事件。
                Wmisc.LOG.debug("[w_misc] 预建 SpearDamageEvent 监听表失败，改为直接注册", seedFailure);
            }
            addDynamicListener(MinecraftForge.EVENT_BUS, this.damageEventType, handler);
        } catch (Throwable t) {
            // 拿不到事件类的监听表时只损失「秒杀小动物」：蓄力、动画、攻击力都不受影响。
            Wmisc.LOG.warn("[w_misc] SpearDamageEvent 挂不上 Forge 事件总线，Mowzie 长矛的秒杀小动物效果停用；其余长矛逻辑照常", t);
        }
    }

    /**
     * 预建事件类的 ListenerList，绕开 Forge 事件总线的「无参构造」硬要求。
     *
     * <p>eventbus 6.x 注册监听器时走 {@code EventListenerHelper.computeListenerList}：对非抽象事件类，
     * 它会先 {@code getConstructor()} 实例化一个事件对象，再拿这个对象的 ListenerList。
     * SpearCore 的 {@code SpearDamageEvent} 只有带参构造，于是这里直接注册会抛
     * {@code NoSuchMethodException}（包在 RuntimeException 里），整个兼容模块在构造期就挂掉。</p>
     *
     * <p>修法：先走「事件实例自己查表」的那条路径（{@code fromInstanceCall=true}）把 ListenerList
     * 建好塞进缓存 —— 它和事件实例 POST 时查到的是同一个对象，之后注册就不需要实例化事件了。</p>
     */
    private static void seedListenerList(Class<?> eventType) throws Exception {
        Method internal = EventListenerHelper.class.getDeclaredMethod("getListenerListInternal", Class.class, boolean.class);
        internal.setAccessible(true);
        internal.invoke((Object)null, eventType, true);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addDynamicListener(IEventBus bus, Class<?> eventType, Consumer<Object> handler) {
        // 事件类型是运行期反射拿到的，泛型对不上，这里按原样交给 Forge 的通用注册入口。
        bus.addListener(EventPriority.NORMAL, false, (Class) eventType, (Consumer) handler);
    }

    public boolean isStab(Object event) {
        try {
            return this.damageAttackType.invoke(event) == this.stabAttackType;
        } catch (Throwable var3) {
            return false;
        }
    }

    public LivingEntity attacker(Object event) {
        try {
            Object value = this.damageAttacker.invoke(event);
            return value instanceof LivingEntity living ? living : null;
        } catch (Throwable var4) {
            return null;
        }
    }

    public Entity target(Object event) {
        try {
            Object value = this.damageTarget.invoke(event);
            return value instanceof Entity entity ? entity : null;
        } catch (Throwable var4) {
            return null;
        }
    }

    public float baseDamage(Object event) {
        try {
            Object value = this.damageBaseDamage.invoke(event);
            return value instanceof Number number ? number.floatValue() : 0.0F;
        } catch (Throwable var4) {
            return 0.0F;
        }
    }

    public void setMultiplier(Object event, float multiplier) {
        try {
            this.damageMultiplierSetter.invoke(event, multiplier);
        } catch (Throwable t) {
            Wmisc.LOG.warn("[w_misc] 修改 SpearDamageEvent 倍率失败，秒杀小动物效果本次不生效", t);
        }

    }
}

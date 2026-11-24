package com.wdlpiaoyi.wmisc.compat.spearcore;

import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.compat.Compat;
import java.util.Collection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.registries.ForgeRegistries;

public final class MowzieSpearCompat {
    public static final String FEATURE_ID = "spearcore_mowzie_spear";
    public static final int CHARGE_USE_DURATION = 72000;
    public static final String MOWZIE_MODID = "mowziesmobs";
    public static final String SPEARCORE_MODID = "spearcore";
    private static final ResourceLocation SPEAR_ID = new ResourceLocation("mowziesmobs", "spear");
    private static final String SPEAR_ITEM_CLASS = "com.bobmowzie.mowziesmobs.server.item.ItemSpear";
    private static final float ANIMAL_HEALTH_LIMIT = 30.0F;
    private static final float ANIMAL_KILL_CHANCE = 0.334F;
    private static final float ANIMAL_KILL_POWER = 400.0F;
    private static volatile boolean installed;
    private static volatile boolean enabled;
    private static SpearCoreBridge bridge;
    private static volatile Item spearItem;
    private static volatile Object spearLike;
    private static volatile boolean spearItemMissingLogged;
    private static boolean animalEffectFailureLogged;

    private MowzieSpearCompat() {
    }

    public static synchronized void install() {
        if (!installed) {
            installed = true;
            if (Compat.isModLoaded("mowziesmobs") && Compat.isModLoaded("spearcore")) {
                bridge = SpearCoreBridge.probe();
                if (bridge != null) {
                    MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ItemAttributeModifierEvent.class, MowzieSpearCompat::onItemAttributeModifiers);
                    bridge.listenSpearDamage(MowzieSpearCompat::onSpearDamage);
                    Wmisc.LOG.info("[w_misc] Mowzie 长矛兼容已接上 SpearCore：左键戳刺 / 右键蓄力 / 第一·第三人称动画走长矛逻辑，攻击力修饰符照抄 spearcore:wooden_spear");
                }
            } else {
                Wmisc.LOG.warn("[w_misc] {}：缺少 mowziesmobs 或 spearcore，兼容停用", "spearcore_mowzie_spear");
            }
        }
    }

    public static void setEnabled(boolean on) {
        boolean next = on && installed && bridge != null;
        if (next != enabled) {
            enabled = next;
            Wmisc.LOG.info("[w_misc] Mowzie 长矛兼容：{}", enabled ? "启用" : "停用");
        }
    }

    public static boolean isInstalled() {
        return installed && bridge != null;
    }

    public static boolean isActive() {
        return enabled && installed && bridge != null;
    }

    public static boolean isMowzieSpearClass(String className) {
        return "com.bobmowzie.mowziesmobs.server.item.ItemSpear".equals(className);
    }

    public static boolean isSpear(ItemStack stack) {
        return enabled && isSpearItem(stack);
    }

    public static boolean isSpearItem(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            Item item = spearItem();
            return item != null && stack.getItem() == item;
        } else {
            return false;
        }
    }

    private static Item spearItem() {
        Item cached = spearItem;
        if (cached != null) {
            return cached;
        } else {
            Item found = ForgeRegistries.ITEMS == null ? null : (Item)ForgeRegistries.ITEMS.getValue(SPEAR_ID);
            if (found != null && found != Items.AIR) {
                spearItem = found;
                return found;
            } else {
                if (!spearItemMissingLogged) {
                    spearItemMissingLogged = true;
                    Wmisc.LOG.warn("[w_misc] 注册表里找不到 {}，Mowzie 长矛兼容等它出现后再生效", SPEAR_ID);
                }

                return null;
            }
        }
    }

    public static Object spearLikeFor(ItemStack stack) {
        if (enabled && bridge != null && isSpearItem(stack)) {
            Object cached = spearLike;
            if (cached == null) {
                cached = bridge.newSpearLike();
                if (cached == null) {
                    return null;
                }

                spearLike = cached;
                Wmisc.LOG.info("[w_misc] mowziesmobs:spear 已接入 SpearCore 的长矛逻辑（SpearLike 代理 → spearcore:wooden_spear）");
            }

            return cached;
        } else {
            return null;
        }
    }

    public static InteractionResultHolder<ItemStack> tryStartCharging(Level level, Player player, InteractionHand hand, ItemStack stack) {
        Object like = spearLikeFor(stack);
        return like == null ? null : bridge.tryStartCharging(level, player, hand, stack, like);
    }

    public static void tickCharge(LivingEntity user, ItemStack stack) {
        Object like = spearLikeFor(stack);
        if (like != null) {
            bridge.tickCharge(user, stack, like);
        }
    }

    private static void onItemAttributeModifiers(ItemAttributeModifierEvent event) {
        if (enabled && bridge != null) {
            if (event.getSlotType() == EquipmentSlot.MAINHAND && isSpearItem(event.getItemStack())) {
                Item wooden = bridge.woodenSpear();
                if (wooden != null) {
                    Collection<AttributeModifier> woodenDamage = wooden.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE);
                    if (!woodenDamage.isEmpty()) {
                        event.removeAttribute(Attributes.ATTACK_DAMAGE);

                        for(AttributeModifier modifier : woodenDamage) {
                            event.addModifier(Attributes.ATTACK_DAMAGE, modifier);
                        }

                    }
                }
            }
        }
    }

    private static void onSpearDamage(Object event) {
        if (enabled && bridge != null) {
            try {
                if (!bridge.isStab(event)) {
                    return;
                }

                LivingEntity var2 = bridge.attacker(event);
                if (!(var2 instanceof Player)) {
                    return;
                }

                Player player = (Player)var2;
                if (!isSpearItem(player.getMainHandItem())) {
                    return;
                }

                Entity var3 = bridge.target(event);
                if (!(var3 instanceof Animal)) {
                    return;
                }

                Animal animal = (Animal)var3;
                if (animal.getHealth() > 30.0F) {
                    return;
                }

                if (animal.level().getRandom().nextFloat() > 0.334F) {
                    return;
                }

                float base = bridge.baseDamage(event);
                bridge.setMultiplier(event, 400.0F / Math.max(0.25F, base));
            } catch (Throwable t) {
                if (!animalEffectFailureLogged) {
                    animalEffectFailureLogged = true;
                    Wmisc.LOG.warn("[w_misc] Mowzie 长矛的秒杀小动物效果处理失败，本次命中按普通伤害结算", t);
                }
            }

        }
    }
}

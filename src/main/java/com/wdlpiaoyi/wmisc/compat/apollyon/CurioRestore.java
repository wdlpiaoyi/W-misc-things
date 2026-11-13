package com.wdlpiaoyi.wmisc.compat.apollyon;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.feature.impl.ApollyonCurioReturnFeature;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotAttribute;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 把「饰品储存水晶」里的东西按原槽位装回去，替代原本「一件件掉到脚下」的行为。
 *
 * <p>两个关键点：</p>
 * <ul>
 *   <li>槽位提供者必须先装：槽位数由 SlotAttribute 动态计算，提供者的加成要等下一个
 *       {@code LivingTickEvent} 才会生效，所以装完要手动 {@code addTransientSlotModifiers}。</li>
 *   <li>原槽位优先：用 {@link CurioSnapshot} 的快照精确还原；没有快照或原位被占时退化为
 *       按物品合法类型找第一个空槽。</li>
 * </ul>
 */
public final class CurioRestore {
    private CurioRestore() {
    }

    /** 水晶被使用时调用（服务端）。返回是否处理了物品。 */
    public static boolean restore(ServerPlayer player, ItemStack crystal) {
        List<ItemStack> stored = readStored(crystal);
        if (stored.isEmpty()) {
            return false;
        }
        ICuriosItemHandler handler = CuriosApi.getCuriosInventory(player).resolve().orElse(null);
        List<CurioSnapshot.Entry> snapshot = ApollyonCurioReturnFeature.preferOriginalSlots()
                ? CurioSnapshot.load(player) : List.of();

        List<ItemStack> pool = new ArrayList<>(stored.size());
        for (ItemStack stack : stored) {
            pool.add(stack.copy());
        }

        boolean[] used = new boolean[pool.size()];
        List<ItemStack> overflow = new ArrayList<>();

        // 槽位提供者优先：否则「额外槽位」里的东西放不进去。
        List<CurioSnapshot.Entry> ordered = new ArrayList<>(snapshot);
        ordered.sort(Comparator.comparingInt(entry -> providesSlots(player, ItemStack.of(entry.item().copy())) ? 0 : 1));

        for (CurioSnapshot.Entry entry : ordered) {
            int index = findMatch(pool, used, entry.item());
            if (index < 0) {
                continue;
            }
            ItemStack stack = pool.get(index);
            boolean placed;
            if (CurioSnapshot.KIND_ARMOR.equals(entry.kind())) {
                placed = putArmor(player, entry.index(), stack);
            } else {
                placed = handler != null && putCurio(player, handler, entry.slotType(), entry.index(), stack);
            }
            if (!placed) {
                placed = tryAnywhere(player, handler, stack);
            }
            if (placed) {
                used[index] = true;
            } else {
                overflow.add(stack);
            }
        }

        for (int i = 0; i < pool.size(); i++) {
            if (!used[i] && !tryAnywhere(player, handler, pool.get(i))) {
                overflow.add(pool.get(i));
            }
        }

        for (ItemStack stack : overflow) {
            if (ApollyonCurioReturnFeature.dropOverflowToGround() || !player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }

        consumeCrystal(player, crystal);
        CurioSnapshot.clear(player);
        player.containerMenu.broadcastChanges();
        Wmisc.LOG.info("[w_misc] Apollyon 饰品已归还原槽位：{} 件，溢出 {} 件", stored.size(), overflow.size());
        return true;
    }

    private static List<ItemStack> readStored(ItemStack crystal) {
        List<ItemStack> stored = new ArrayList<>();
        if (!crystal.hasTag() || !crystal.getTag().contains("BannedCurios", Tag.TAG_LIST)) {
            return stored;
        }
        ListTag list = crystal.getTag().getList("BannedCurios", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.of(list.getCompound(i));
            if (!stack.isEmpty()) {
                stored.add(stack);
            }
        }
        return stored;
    }

    /** 清空水晶；非创造模式下按原逻辑把空水晶消耗掉。 */
    private static void consumeCrystal(ServerPlayer player, ItemStack crystal) {
        if (crystal.hasTag()) {
            crystal.getTag().put("BannedCurios", new ListTag());
        }
        if (!player.getAbilities().instabuild) {
            crystal.shrink(1);
        }
    }

    private static int findMatch(List<ItemStack> pool, boolean[] used, CompoundTag itemTag) {
        for (int i = 0; i < pool.size(); i++) {
            if (!used[i] && pool.get(i).save(new CompoundTag()).equals(itemTag)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean putArmor(ServerPlayer player, int index, ItemStack stack) {
        if (index < 0 || index >= player.getInventory().armor.size()) {
            return false;
        }
        if (!player.getInventory().armor.get(index).isEmpty()) {
            return false;
        }
        player.getInventory().armor.set(index, stack);
        return true;
    }

    private static boolean putCurio(ServerPlayer player, ICuriosItemHandler handler,
                                    String slotType, int index, ItemStack stack) {
        ICurioStacksHandler stacksHandler = handler.getStacksHandler(slotType).orElse(null);
        if (stacksHandler == null) {
            return false;
        }
        IDynamicStackHandler stacks = stacksHandler.getStacks();
        if (index < 0 || index >= stacks.getSlots()) {
            return false;
        }
        if (!stacks.getStackInSlot(index).isEmpty() || !stacks.isItemValid(index, stack)) {
            return false;
        }
        handler.setEquippedCurio(slotType, index, stack);
        applySlotModifiersNow(player, handler, slotType, index, stack);
        return true;
    }

    private static boolean tryAnywhere(ServerPlayer player, ICuriosItemHandler handler, ItemStack stack) {
        if (handler != null) {
            for (String slotType : CuriosApi.getItemStackSlots(stack, player).keySet()) {
                ICurioStacksHandler stacksHandler = handler.getStacksHandler(slotType).orElse(null);
                if (stacksHandler == null) {
                    continue;
                }
                IDynamicStackHandler stacks = stacksHandler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    if (stacks.getStackInSlot(i).isEmpty() && stacks.isItemValid(i, stack)) {
                        handler.setEquippedCurio(slotType, i, stack);
                        applySlotModifiersNow(player, handler, slotType, i, stack);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** 立刻把槽位修饰符应用上，让 {@code getStacks()} 在本次 tick 就扩容。 */
    private static void applySlotModifiersNow(ServerPlayer player, ICuriosItemHandler handler,
                                              String slotType, int index, ItemStack stack) {
        SlotContext context = new SlotContext(slotType, player, index, false, true);
        UUID uuid = CuriosApi.getSlotUuid(context);
        Multimap<Attribute, AttributeModifier> modifiers =
                CuriosApi.getAttributeModifiers(context, uuid, stack);
        Multimap<String, AttributeModifier> slotModifiers = HashMultimap.create();
        for (Attribute attribute : new ArrayList<>(modifiers.keySet())) {
            if (attribute instanceof SlotAttribute slotAttribute) {
                slotModifiers.putAll(slotAttribute.getIdentifier(), modifiers.get(attribute));
            }
        }
        if (!slotModifiers.isEmpty()) {
            handler.addTransientSlotModifiers(slotModifiers);
        }
    }

    /** 这件物品是否会给某个槽位类型提供额外槽位。 */
    private static boolean providesSlots(ServerPlayer player, ItemStack stack) {
        for (String slotType : CuriosApi.getItemStackSlots(stack, player).keySet()) {
            SlotContext context = new SlotContext(slotType, player, 0, false, true);
            Multimap<Attribute, AttributeModifier> modifiers =
                    CuriosApi.getAttributeModifiers(context, CuriosApi.getSlotUuid(context), stack);
            for (Attribute attribute : modifiers.keySet()) {
                if (attribute instanceof SlotAttribute) {
                    return true;
                }
            }
        }
        return false;
    }
}

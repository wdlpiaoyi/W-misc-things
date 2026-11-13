package com.wdlpiaoyi.wmisc.compat.apollyon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * 没收前记录「哪一件在哪个槽位」，供战后水晶按原位归还。
 *
 * <p>RevelationFix 只把 ItemStack 的 NBT 存进水晶（没有槽位信息），盔甲和饰品还混在同一个 list 里。
 * 我们在它动手前把「盔甲槽位 + (curio 类型, index)」快照存到玩家的 ForgeData 里，
 * 跟着玩家存档走，死亡/重登都不丢。</p>
 */
public final class CurioSnapshot {
    private static final String KEY = "w_misc:apollyon_curio_snapshot";
    public static final String KIND_ARMOR = "armor";
    public static final String KIND_CURIO = "curio";

    /** 一条快照：盔甲用 kind=armor + index；饰品用 kind=curio + slotType + index。 */
    public record Entry(String kind, String slotType, int index, CompoundTag item) {
    }

    private CurioSnapshot() {
    }

    public static boolean has(ServerPlayer player) {
        return player.getPersistentData().contains(KEY, Tag.TAG_LIST);
    }

    /** 只在还没有快照时记录，避免后续重复调用把原始排列覆盖成空。 */
    public static void capture(ServerPlayer player) {
        if (has(player)) {
            return;
        }
        ListTag list = new ListTag();

        for (int i = 0; i < player.getInventory().armor.size(); i++) {
            ItemStack stack = player.getInventory().armor.get(i);
            if (!stack.isEmpty()) {
                list.add(entry(KIND_ARMOR, "", i, stack));
            }
        }

        CuriosApi.getCuriosInventory(player).resolve().ifPresent(handler -> collectCurios(handler, list));

        if (!list.isEmpty()) {
            player.getPersistentData().put(KEY, list);
        }
    }

    private static void collectCurios(ICuriosItemHandler handler, ListTag out) {
        for (ICurioStacksHandler stacksHandler : handler.getCurios().values()) {
            IDynamicStackHandler stacks = stacksHandler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    out.add(entry(KIND_CURIO, stacksHandler.getIdentifier(), i, stack));
                }
            }
        }
    }

    private static CompoundTag entry(String kind, String slotType, int index, ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        tag.putString("kind", kind);
        tag.putString("slot", slotType);
        tag.putInt("index", index);
        tag.put("item", stack.save(new CompoundTag()));
        return tag;
    }

    public static List<Entry> load(ServerPlayer player) {
        List<Entry> entries = new ArrayList<>();
        if (!has(player)) {
            return entries;
        }
        ListTag list = player.getPersistentData().getList(KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            entries.add(new Entry(
                    tag.getString("kind"),
                    tag.getString("slot"),
                    tag.getInt("index"),
                    tag.getCompound("item")));
        }
        return entries;
    }

    public static void clear(ServerPlayer player) {
        player.getPersistentData().remove(KEY);
    }
}

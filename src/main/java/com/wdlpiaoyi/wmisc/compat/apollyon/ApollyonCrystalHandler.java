package com.wdlpiaoyi.wmisc.compat.apollyon;

import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.feature.impl.ApollyonCurioReturnFeature;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 拦截「饰品储存水晶」的右键使用，改为按原槽位装回。
 *
 * <p>用 Forge 事件而不是 mixin：{@code RightClickBlock} 在 {@code Item#useOn} 之前触发，
 * 取消后原逻辑（一件件掉到地上）就不会执行，零编译期依赖，也不需要 refmap / AT。</p>
 */
public final class ApollyonCrystalHandler {
    private static final ResourceLocation CRYSTAL_ID =
            new ResourceLocation("goety_revelation:curios_storage_crystal");

    private ApollyonCrystalHandler() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!ApollyonCurioReturnFeature.isActive()) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (held.isEmpty() || !isCrystal(held)) {
            return;
        }
        if (!held.hasTag() || !held.getTag().contains("BannedCurios", net.minecraft.nbt.Tag.TAG_LIST)) {
            return;
        }
        if (CurioRestore.restore(player, held)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        } else {
            Wmisc.LOG.debug("[w_misc] 水晶里没有可归还的物品");
        }
    }

    private static boolean isCrystal(ItemStack stack) {
        Item item = ForgeRegistries.ITEMS.getValue(CRYSTAL_ID);
        return item != null && stack.getItem() == item;
    }
}


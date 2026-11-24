package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.compat.spearcore.MowzieSpearCompat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Item.class})
public abstract class MowzieSpearItemMixin {
    @Inject(
        method = {"use"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void wmisc$startSpearCharging(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        InteractionResultHolder<ItemStack> result = MowzieSpearCompat.tryStartCharging(level, player, hand, player.getItemInHand(hand));
        if (result != null) {
            cir.setReturnValue(result);
        }

    }

    @Inject(
        method = {"onUseTick"},
        at = {@At("HEAD")}
    )
    private void wmisc$tickSpearCharge(Level level, LivingEntity user, ItemStack stack, int remainingTicks, CallbackInfo ci) {
        MowzieSpearCompat.tickCharge(user, stack);
    }

    @Inject(
        method = {"getUseDuration"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void wmisc$spearUseDuration(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (MowzieSpearCompat.isSpear(stack)) {
            cir.setReturnValue(72000);
        }

    }
}

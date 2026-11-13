package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.feature.impl.EnigmaticCreativeFeature;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 目标：enigmatic legacy 2.30.1 的
 * {@code com.aizistral.enigmaticlegacy.handlers.SuperpositionHandler#isTheWorthyOne(Player)Z}。
 *
 * <p>这是「佩戴七咒之戒时长是否合格」的唯一判定入口。在 RETURN 把创造模式玩家改成 true；
 * 附属 EnigmaticAddons 也在这个方法的 RETURN 注入（只加 true），两边叠加安全。</p>
 *
 * <p>声明为 static handler，因为目标方法是 public static；目标类不在编译期类路径上，用
 * {@code targets} 字符串 + {@code @Pseudo}，模组缺失时不会崩。</p>
 */
@Pseudo
@Mixin(targets = "com.aizistral.enigmaticlegacy.handlers.SuperpositionHandler", remap = false)
public abstract class MixinSuperpositionHandlerWorthy {
    @Inject(method = "isTheWorthyOne", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void wmisc$creativeSkipsPlaytimeGate(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue()) || player == null) {
            return;
        }
        if (EnigmaticCreativeFeature.shouldBypassWorthyGate(player)) {
            cir.setReturnValue(true);
        }
    }
}

package com.wdlpiaoyi.wmisc.mixin;

import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Quark 的 Q 按钮在特定月份会变成彩虹色（QButton#getFGColor 返回 RAINBOW）。
 *
 * <p>这里把前景色固定成白色，按钮仍可点击、功能不变，只是不再做节日彩虹展示。
 * 目标是客户端类，放在 mixin 配置的 client 段；{@code @Pseudo} 让没装 Quark 时安全跳过。</p>
 */
@Pseudo
@Mixin(targets = "org.violetmoon.quark.base.client.config.QButton", remap = false)
public abstract class QuarkQButtonMixin {
    @Inject(method = "getFGColor", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void wmisc$neutralColor(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0xFFFFFF);
    }
}

package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.compat.content.ContentFilterRules;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Malum 的 cosmetickin 注册：{@code ArmorSkinRegistry.registerPridewear(name, item)} 会用 prideweave
 * 物品注册「pride 织纹」护甲皮肤。物品已经从注册表跳过（拿到的是 air），这里直接把注册调用变成 no-op，
 * 避免把 18 个皮肤都挂到 air 上产生重复/无效数据。
 */
@Pseudo
@Mixin(targets = "com.sammy.malum.registry.common.item.ArmorSkinRegistry", remap = false)
public abstract class MalumPrideArmorSkinMixin {
    @Inject(method = "registerPridewear", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void wmisc$skipPrideSkins(String name, Item item, CallbackInfo ci) {
        if (ContentFilterRules.isActive()) {
            ci.cancel();
        }
    }
}

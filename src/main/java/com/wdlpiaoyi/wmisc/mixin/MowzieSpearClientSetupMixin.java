package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.compat.spearcore.MowzieSpearCompat;
import com.wdlpiaoyi.wmisc.compat.spearcore.client.MowzieSpearClientExtensions;
import java.util.function.Consumer;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Item.class})
public abstract class MowzieSpearClientSetupMixin {
    @Inject(
        method = {"initializeClient"},
        at = {@At("HEAD")},
        cancellable = true,
        remap = false
    )
    private void wmisc$registerSpearHandTransform(Consumer<IClientItemExtensions> consumer, CallbackInfo ci) {
        Item self = (Item) (Object) this;
        if (MowzieSpearCompat.isInstalled() && MowzieSpearCompat.isMowzieSpearClass(self.getClass().getName())) {
            consumer.accept(MowzieSpearClientExtensions.create());
            ci.cancel();
        }
    }
}

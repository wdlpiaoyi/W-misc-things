package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.compat.content.ContentFilterRules;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

/**
 * 从 Malum 的注册表里直接跳过带 {@code pride} 的物品（prideweave 与 pride 服饰）。
 *
 * <p>Malum 所有物品都走 {@code ItemRegistry.register(String, Properties, Function)}，这里按名字拦截：
 * 直接不注册，返回一个指向 {@code minecraft:air} 的 {@link RegistryObject}，让 Malum 里
 * {@code ItemRegistry.XXX_PRIDEWEAVE.get()} 之类的引用仍能拿到东西（不会 NPE/抛"Registry entry not present"），
 * 但物品本身不再存在于注册表：JEI/EMI 不会显示，/give 也拿不到，旧存档里的它们会消失。</p>
 *
 * <p>Malum 里引用这些物品的地方只有 {@code ArmorSkinRegistry}（由配套的 mixin 跳过）
 * 和 datagen 的 {@code MalumItemModels}（运行时不执行）。</p>
 */
@Pseudo
@Mixin(targets = "com.sammy.malum.registry.common.item.ItemRegistry", remap = false)
public abstract class MalumPrideItemRemovalMixin {
    @Unique
    private static RegistryObject<Item> wmisc$airHolder;

    @Inject(method = "register", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void wmisc$skipPrideItems(String name, Item.Properties properties, Function factory,
                                             CallbackInfoReturnable<RegistryObject> cir) {
        if (!ContentFilterRules.isBlockedMalumItemName(name)) {
            return;
        }
        ContentFilterRules.noteSkippedItem(name);
        cir.setReturnValue(airHolder());
    }

    @Unique
    private static RegistryObject<Item> airHolder() {
        if (wmisc$airHolder == null) {
            wmisc$airHolder = RegistryObject.create(new ResourceLocation("minecraft", "air"), ForgeRegistries.ITEMS);
        }
        return wmisc$airHolder;
    }
}

package com.wdlpiaoyi.wmisc.mixin;

import com.mega.revelationfix.common.apollyon.common.PlayerTickrateExecutor;
import com.wdlpiaoyi.wmisc.compat.apollyon.CurioSnapshot;
import com.wdlpiaoyi.wmisc.feature.impl.ApollyonCurioReturnFeature;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 RevelationFix 的 Apollyon 没收逻辑执行前，把当前盔甲/饰品的槽位快照存下来。
 *
 * <p>目标：{@code PlayerTickrateExecutor#processInventory(Player, InventoryWork)}（private static）。
 * 该方法头部做快照时，物品还在原位；没收开始后 {@code lambda$processInventory$2} 会直接清空 curios 槽位，
 * 之后就拿不到原始 index 了。</p>
 *
 * <p>{@code InventoryWork} 来自 jar-in-jar 的 RevelationFix，编译期用 src/stubs 的空壳类满足类型，
 * 不进 jar；{@code @Pseudo} 让目标缺失时安全跳过。</p>
 */
@Pseudo
@Mixin(targets = "com.mega.revelationfix.common.apollyon.common.PlayerTickrateExecutor", remap = false)
public abstract class ApollyonConfiscateSnapshotMixin {
    @Inject(method = "processInventory", at = @At("HEAD"), remap = false, require = 0)
    private static void wmisc$snapshotBeforeConfiscation(Player player, PlayerTickrateExecutor.InventoryWork work,
                                                         CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer && ApollyonCurioReturnFeature.isActive()) {
            CurioSnapshot.capture(serverPlayer);
        }
    }
}

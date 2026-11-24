package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.compat.irons.AnvilJeiBudget;
import com.wdlpiaoyi.wmisc.compat.irons.AnvilRecipeView;
import com.wdlpiaoyi.wmisc.compat.irons.AnvilTupleView;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
    targets = {"io.redspace.ironsspellbooks.jei.ArcaneAnvilJeiRecipe"},
    remap = false
)
public abstract class IronsAnvilRecipeMixin implements AnvilRecipeView {
    @Shadow
    Item leftItem;
    @Shadow
    Item rightItem;
    @Unique
    private Object wmisc$cachedTuple;
    @Unique
    private boolean wmisc$trimmed;
    @Unique
    private static boolean wmisc$trimFailureLogged;

    @Inject(
        method = {"getRecipeItems"},
        at = {@At("HEAD")},
        cancellable = true,
        remap = false,
        require = 0
    )
    private void wmisc$reuseTuple(CallbackInfoReturnable<Object> cir) {
        Object cached = this.wmisc$cachedTuple;
        if (cached != null) {
            cir.setReturnValue(cached);
        }

    }

    @Inject(
        method = {"getRecipeItems"},
        at = {@At("RETURN")},
        remap = false,
        require = 0
    )
    private void wmisc$trimAndCache(CallbackInfoReturnable<Object> cir) {
        Object tuple = cir.getReturnValue();
        if (tuple != null) {
            if (!this.wmisc$trimmed && tuple instanceof AnvilTupleView) {
                AnvilTupleView view = (AnvilTupleView)tuple;
                this.wmisc$trimmed = true;

                try {
                    AnvilJeiBudget.trimTuple(view);
                } catch (RuntimeException broken) {
                    this.wmisc$trimmed = false;
                    if (!wmisc$trimFailureLogged) {
                        wmisc$trimFailureLogged = true;
                        Wmisc.LOG.warn("[w_misc] 奥术铁砧配方槽位裁剪失败，按原样注册", broken);
                    }
                }
            }

            this.wmisc$cachedTuple = tuple;
        }
    }

    public Item wmisc$leftItem() {
        return this.leftItem;
    }

    public Item wmisc$rightItem() {
        return this.rightItem;
    }
}

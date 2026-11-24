package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.compat.irons.AnvilTupleView;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(
    targets = {"io.redspace.ironsspellbooks.jei.ArcaneAnvilJeiRecipe$Tuple"},
    remap = false
)
public abstract class IronsAnvilTupleMixin implements AnvilTupleView {
    @Shadow
    public abstract Object a();

    @Shadow
    public abstract Object b();

    @Shadow
    public abstract Object c();

    public List<?> wmisc$leftInputs() {
        return (List)this.a();
    }

    public List<?> wmisc$rightInputs() {
        return (List)this.b();
    }

    public List<?> wmisc$outputs() {
        return (List)this.c();
    }
}

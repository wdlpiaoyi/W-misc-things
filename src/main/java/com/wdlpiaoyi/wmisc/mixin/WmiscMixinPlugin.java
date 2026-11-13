package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.compat.Compat;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 最小化的、零第三方依赖的 Mixin 软开关。
 *
 * <p>Mixin 配置 {@code w_misc.mixins.json} 里 {@code required=false}：目标模组没装时只跳过对应注入，
 * 不会因为找不到目标类把启动搞崩。目标类在编译期都不可见，所以 mixin 本身用
 * {@code targets} 字符串 + {@code @Pseudo}。</p>
 *
 * <p>新增需要前置的 mixin：在 {@link #REQUIRED_MODS} 里登记 简单类名 → 需要的模组 id。
 * 这里只能做「模组是否安装」的判断：配置在此时还没加载，模块级开关请在运行时判断。</p>
 */
public final class WmiscMixinPlugin implements IMixinConfigPlugin {
    /** mixin 简单类名 → 需要的模组 id。没登记的 mixin 一律应用。 */
    private static final Map<String, List<String>> REQUIRED_MODS = Map.of(
            "IceAndFireInitMixin", List.of("iceandfire"),
            "L2WeaponryItemsMixin", List.of("l2weaponry"),
            "ApollyonConfiscateSnapshotMixin", List.of("revelationfix"),
            "MixinSuperpositionHandlerWorthy", List.of("enigmaticlegacy"),
            "QuarkQButtonMixin", List.of("quark")
    );

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String simpleName = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        List<String> required = REQUIRED_MODS.get(simpleName);
        if (required == null) {
            return true;
        }
        for (String modId : required) {
            if (!Compat.isModLoaded(modId)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}

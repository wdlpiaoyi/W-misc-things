package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.compat.Compat;
import com.wdlpiaoyi.wmisc.compat.l2modularblock.L2ModularBlockFinalPatch;
import com.wdlpiaoyi.wmisc.compat.mixin.MixinDetacher;
import com.wdlpiaoyi.wmisc.util.ConfigPeek;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 最小化的、零第三方依赖的 Mixin 软开关。
 *
 * <p>Mixin 配置 {@code w_misc.mixins.json} 里 {@code required=false}：目标模组没装时只跳过对应注入，
 * 不会因为找不到目标类把启动搞崩。目标类在编译期都不可见，所以 mixin 本身用
 * {@code targets} 字符串 + {@code @Pseudo}。</p>
 *
 * <p>新增需要前置的 mixin：在 {@link #REQUIRED_MODS} 里登记 简单类名 → 需要的模组 id。
 * 这里只能做「模组是否安装」的判断：配置在此时还没加载，模块级开关请在运行时判断。</p>
 *
 * <p>另外这里挂了一个「改写被注入类本身」的补丁：{@link L2ModularBlockFinalPatch} 在
 * {@link #preApply} 里摘掉 l2modularblock 目标方法的 final 标记，让 l2weaponry 的分发 mixin
 * 能覆盖它们。</p>
 */
public final class WmiscMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOG = LoggerFactory.getLogger("w_misc/l2modularblock");

    /** l2modularblock 补丁自己的配置段；构造期就要知道开关，所以走 ConfigPeek。 */
    private static final String SECTION = "features.l2modularblock_final_fix";

    /** mixin 简单类名 → 需要的模组 id。没登记的 mixin 一律应用。 */
    private static final Map<String, List<String>> REQUIRED_MODS = Map.ofEntries(
            Map.entry("IceAndFireInitMixin", List.of("iceandfire")),
            Map.entry("L2WeaponryItemsMixin", List.of("l2weaponry")),
            Map.entry("L2WeaponryCompatDispatchMixin", List.of("l2weaponry")),
            Map.entry("ApollyonConfiscateSnapshotMixin", List.of("revelationfix")),
            Map.entry("MixinSuperpositionHandlerWorthy", List.of("enigmaticlegacy")),
            Map.entry("QuarkQButtonMixin", List.of("quark")),
            Map.entry("MalumPrideItemRemovalMixin", List.of("malum")),
            Map.entry("MalumPrideArmorSkinMixin", List.of("malum")),
            Map.entry("ImmersiveEngineeringShaderMixin", List.of("immersiveengineering")),
            Map.entry("IronsAnvilRecipeMakerMixin", List.of("irons_spellbooks")),
            Map.entry("IronsAnvilRecipeMixin", List.of("irons_spellbooks")),
            Map.entry("IronsAnvilTupleMixin", List.of("irons_spellbooks")),
            Map.entry("IronsAnvilJeiRegistrationMixin", List.of("irons_spellbooks")),
            Map.entry("WtcResourceBarMixin", List.of("wizard_terra_cuiros")),
            Map.entry("GgtuGoeticDimensionMixin", List.of("goeticlegacy", "resolutionscaler")),
            Map.entry("MowzieSpearLikeSupportMixin", List.of("mowziesmobs", "spearcore")),
            Map.entry("SculptorTestCheatMixin", List.of("mowziesmobs")),
            Map.entry("SculptorTradeHoverMixin", List.of("mowziesmobs")),
            Map.entry("FantasyGeckolibAnimTimeMixin", List.of("fantasy_ending", "geckolib")),
            Map.entry("ItemRendererAfterimageRedirectMixin", List.of("saintsdragons"))
    );

    /**
     * mixin 简单类名 → 模块 id。模块在配置里被停用时这条 mixin 不应用。
     *
     * <p>这类 mixin 在 {@code ItemRenderer} 类加载期就位，所以开关只能启动前改（{@link ConfigPeek}），
     * 运行期热改无效。</p>
     */
    private static final Map<String, String> FEATURE_GATED = Map.of(
            "ItemRendererAfterimageRedirectMixin", "carbon_saintsdragons_foil");

    /**
     * 要从 saintsdragons 配置里摘掉的 mixin：它在 {@code ItemRenderer#render} 上 redirect
     * {@code hasFoil}，和 carbon 的 {@code imo$foil} 抢同一条指令，而 carbon 那条是
     * {@code require = 1}，被跳过就会让游戏起不来。摘掉后由 {@code ItemRendererAfterimageRedirectMixin}
     * 复刻它的其余三条注入，{@code hasFoil} 那条让给 carbon。
     */
    private static final MixinDetacher.Spec SAINTSDRAGONS_ITEM_RENDERER = new MixinDetacher.Spec(
            "saintsdragons.mixins.json",
            "com.leon.saintsdragons.forge.mixin.client.BloodTempestAfterimageItemRendererMixin",
            "client.BloodTempestAfterimageItemRendererMixin",
            "net.minecraft.client.renderer.entity.ItemRenderer");

    /** 摘除只做一次；acceptTargets 每个 Mixin 阶段都会调用。 */
    private static boolean saintsDragonsDetached;

    /** 已经打过补丁的目标类，避免重复刷日志。 */
    private static final Set<String> PATCHED = ConcurrentHashMap.newKeySet();

    /** 启动期自证：我们的 ItemRenderer mixin 真的合入了。 */
    private static final AtomicBoolean ITEM_RENDERER_PROBE = new AtomicBoolean();

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
        String featureId = FEATURE_GATED.get(simpleName);
        if (featureId != null && !featureEnabled(featureId)) {
            return false;
        }
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

    /** 启动前读配置文件判断模块开关；读不到就按默认启用。 */
    private static boolean featureEnabled(String featureId) {
        try {
            return ConfigPeek.getBoolean("features." + featureId, "enabled", true);
        } catch (Throwable ignored) {
            return true;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // 这里所有配置都已 prepare、还没有任何类被应用 mixin，是唯一能赶在 ItemRenderer
        // 被变换前摘掉 saintsdragons 那条冲突 mixin 的钩子（它比 mod 构造还早）。
        detachSaintsDragonsItemRenderer();
    }

    private static synchronized void detachSaintsDragonsItemRenderer() {
        if (saintsDragonsDetached) {
            return;
        }
        if (!featureEnabled("carbon_saintsdragons_foil")) {
            LOG.info("[carbon_saintsdragons_foil] 配置里已停用，不摘除 saintsdragons 的 ItemRenderer mixin");
            return;
        }
        try {
            MixinDetacher.Status status = MixinDetacher.run("prepare", SAINTSDRAGONS_ITEM_RENDERER, false);
            if (status != MixinDetacher.Status.NOT_READY) {
                saintsDragonsDetached = true;
            }
            LOG.info("[carbon_saintsdragons_foil] 摘除 saintsdragons 的 ItemRenderer mixin：{}"
                    + "（hasFoil 那条留给 carbon，其余三条由本模组复刻）", status);
        } catch (Throwable t) {
            LOG.error("[carbon_saintsdragons_foil] 摘除 saintsdragons 的 ItemRenderer mixin 失败，"
                    + "carbon 的 imo$foil 仍可能因冲突被跳过后崩档", t);
        }
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (mixinClassName.endsWith("ItemRendererAfterimageRedirectMixin")
                && "net.minecraft.client.renderer.entity.ItemRenderer".equals(targetClassName)
                && ITEM_RENDERER_PROBE.compareAndSet(false, true)) {
            LOG.info("[carbon_saintsdragons_foil] ItemRendererAfterimageRedirectMixin 已合入 ItemRenderer"
                    + "（priority 1500）：接管 getRenderTypes / hasAnimatedTexture / getFoilBuffer*，"
                    + "carbon 的 hasFoil 保持独占");
        }
        if (!L2ModularBlockFinalPatch.handles(targetClassName) || !enabled()) {
            return;
        }
        try {
            List<String> changed = L2ModularBlockFinalPatch.stripFinal(targetClass);
            if (changed.isEmpty()) {
                return;
            }
            if (PATCHED.add(targetClassName)) {
                LOG.info("[l2modularblock] 已摘除 {} 个方法的 final 标记（{}）：{}",
                        changed.size(), targetClassName, String.join(", ", changed));
            }
        } catch (Throwable t) {
            LOG.error("[l2modularblock] 摘 final 失败：{}", targetClassName, t);
        }
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean enabled() {
        try {
            return ConfigPeek.getBoolean(SECTION, "enabled", true);
        } catch (Throwable ignored) {
            return true;
        }
    }
}

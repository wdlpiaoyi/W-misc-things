package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.compat.content.ContentFilterRules;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 在资源管理器返回资源之后、上层解析之前改写内容。
 *
 * <p>为什么不用资源包覆盖：跨模组的资源包顺序不可控（同优先级谁赢取决于 Forge 的 mod 顺序），
 * 换实例就要重新摆包；在 {@code MultiPackResourceManager} 这一层改写，与包顺序无关。</p>
 *
 * <p>两个路径都要接：单项读取（贴图 / splash）走 {@code getResource}，
 * 目录批量读取（配方 / 进度 / 语言）走 {@code listResources}。</p>
 */
@Mixin(MultiPackResourceManager.class)
public abstract class ResourceManagerFilterMixin {
    @Inject(method = "getResource", at = @At("RETURN"), cancellable = true)
    private void wmisc$filterSingle(ResourceLocation id,
                                    CallbackInfoReturnable<Optional<Resource>> cir) {
        Optional<Resource> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) {
            return;
        }
        ContentFilterRules.Rule rule = ContentFilterRules.match(id);
        if (rule == null) {
            return;
        }
        ContentFilterRules.rewrite(original.get(), rule).ifPresent(replaced -> cir.setReturnValue(Optional.of(replaced)));
    }

    /**
     * 语言文件不走 getResource：{@code ClientLanguage.loadFrom} 用 {@code getResourceStack} 把各资源包的
     * 同名文件全部取出来再叠加。这里把栈里每一份都改写，保证最后叠加出来的结果也是中性文本。
     */
    @Inject(method = "getResourceStack", at = @At("RETURN"), cancellable = true)
    private void wmisc$filterStack(ResourceLocation id,
                                   CallbackInfoReturnable<java.util.List<Resource>> cir) {
        java.util.List<Resource> stack = cir.getReturnValue();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        ContentFilterRules.Rule rule = ContentFilterRules.match(id);
        if (rule == null) {
            return;
        }
        java.util.List<Resource> replaced = new java.util.ArrayList<>(stack.size());
        boolean changed = false;
        for (Resource resource : stack) {
            var rewritten = ContentFilterRules.rewrite(resource, rule);
            if (rewritten.isPresent()) {
                replaced.add(rewritten.get());
                changed = true;
            } else {
                replaced.add(resource);
            }
        }
        if (changed) {
            cir.setReturnValue(replaced);
        }
    }

    @Inject(method = "listResources", at = @At("RETURN"), cancellable = true)
    private void wmisc$filterAll(String directory, Predicate<ResourceLocation> filter,
                                 CallbackInfoReturnable<Map<ResourceLocation, Resource>> cir) {
        Map<ResourceLocation, Resource> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) {
            return;
        }
        Map<ResourceLocation, Resource> replaced = ContentFilterRules.rewriteAll(original);
        if (replaced != original) {
            cir.setReturnValue(replaced);
        }
    }
}

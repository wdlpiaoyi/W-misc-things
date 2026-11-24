package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.compat.content.ContentFilterRules;
import com.wdlpiaoyi.wmisc.compat.data.ConditionalResources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 在资源管理器返回资源之后、上层解析之前改写、删除或隐藏内容。
 *
 * <p>为什么不用资源包覆盖：跨模组的资源包顺序不可控（同优先级谁赢取决于 Forge 的 mod 顺序），
 * 换实例就要重新摆包；在 {@code MultiPackResourceManager} 这一层处理，与包顺序无关。</p>
 *
 * <p>两类处理：</p>
 * <ul>
 *   <li>{@link ContentFilterRules}：内容过滤规则（删除/改写第三方宣传资源）；</li>
 *   <li>{@link ConditionalResources}：随包携带但依赖前置的数据文件（前置缺失时假装它不存在）。</li>
 * </ul>
 *
 * <p>四个入口都要接：单项读取（贴图 / splash）走 {@code getResource}；目录批量读取（配方 / 进度 /
 * 数据包注册表）走 {@code listResources}；语言文件走 {@code getResourceStack}（ClientLanguage 会把
 * 各包的同一文件叠加）；{@code listResourceStacks} 是批量读取的多资源版本。</p>
 */
@Mixin(MultiPackResourceManager.class)
public abstract class ResourceManagerFilterMixin {
    @Inject(method = "getResource", at = @At("RETURN"), cancellable = true)
    private void wmisc$filterSingle(ResourceLocation id,
                                    CallbackInfoReturnable<Optional<Resource>> cir) {
        if (ConditionalResources.isHidden(id)) {
            cir.setReturnValue(Optional.empty());
            return;
        }
        Optional<Resource> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) {
            return;
        }
        ContentFilterRules.Rule rule = ContentFilterRules.match(id);
        if (rule == null) {
            return;
        }
        if (ContentFilterRules.isRemoval(rule)) {
            cir.setReturnValue(Optional.empty());
            return;
        }
        ContentFilterRules.rewrite(original.get(), rule).ifPresent(replaced -> cir.setReturnValue(Optional.of(replaced)));
    }

    /** 语言文件：把资源栈里每一份都改写，保证叠加后的结果也是中性文本。 */
    @Inject(method = "getResourceStack", at = @At("RETURN"), cancellable = true)
    private void wmisc$filterStack(ResourceLocation id,
                                   CallbackInfoReturnable<List<Resource>> cir) {
        if (ConditionalResources.isHidden(id)) {
            cir.setReturnValue(List.of());
            return;
        }
        List<Resource> stack = cir.getReturnValue();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        ContentFilterRules.Rule rule = ContentFilterRules.match(id);
        if (rule == null) {
            return;
        }
        if (ContentFilterRules.isRemoval(rule)) {
            cir.setReturnValue(List.of());
            return;
        }
        List<Resource> replaced = new ArrayList<>(stack.size());
        boolean changed = false;
        for (Resource resource : stack) {
            Optional<Resource> rewritten = ContentFilterRules.rewrite(resource, rule);
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

    /** 配方 / 进度 / 数据包注册表这类按目录批量读取的资源：整条删除或逐条改写。 */
    @Inject(method = "listResources", at = @At("RETURN"), cancellable = true)
    private void wmisc$filterAll(String directory, Predicate<ResourceLocation> filter,
                                 CallbackInfoReturnable<Map<ResourceLocation, Resource>> cir) {
        Map<ResourceLocation, Resource> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) {
            return;
        }
        Map<ResourceLocation, Resource> out = null;
        for (Map.Entry<ResourceLocation, Resource> entry : original.entrySet()) {
            boolean hidden = ConditionalResources.isHidden(entry.getKey());
            ContentFilterRules.Rule rule = hidden ? null : ContentFilterRules.match(entry.getKey());
            if (!hidden && rule == null) {
                continue;
            }
            if (out == null) {
                out = new HashMap<>(original);
            }
            if (hidden || ContentFilterRules.isRemoval(rule)) {
                out.remove(entry.getKey());
                continue;
            }
            Optional<Resource> rewritten = ContentFilterRules.rewrite(entry.getValue(), rule);
            if (rewritten.isPresent()) {
                out.put(entry.getKey(), rewritten.get());
            }
        }
        if (out != null) {
            cir.setReturnValue(out);
        }
    }

    /** 多资源版本：只处理「整条隐藏」，内容过滤的改写走单资源那几个入口。 */
    @Inject(method = "listResourceStacks", at = @At("RETURN"), cancellable = true)
    private void wmisc$filterStacks(String directory, Predicate<ResourceLocation> filter,
                                    CallbackInfoReturnable<Map<ResourceLocation, List<Resource>>> cir) {
        Map<ResourceLocation, List<Resource>> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) {
            return;
        }
        Map<ResourceLocation, List<Resource>> out = null;
        for (ResourceLocation id : original.keySet()) {
            if (!ConditionalResources.isHidden(id)) {
                continue;
            }
            if (out == null) {
                out = new HashMap<>(original);
            }
            out.remove(id);
        }
        if (out != null) {
            cir.setReturnValue(out);
        }
    }
}

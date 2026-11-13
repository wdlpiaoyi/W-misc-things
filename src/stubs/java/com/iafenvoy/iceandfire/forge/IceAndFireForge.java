package com.iafenvoy.iceandfire.forge;

/**
 * 仅用于 Mixin 注解处理器校验注入目标：给 {@code IceAndFireConstructMixin} 的
 * {@code @Mixin(targets = "...")} 提供一个同名类。
 *
 * <p>不会被编译进本模组的 jar，也不会在运行时出现（sourceSet 是 stubs，只进 compileOnly 类路径）。
 * 真正的类来自玩家的 iceandfire。</p>
 */
public class IceAndFireForge {
    public IceAndFireForge() {
    }
}

package dev.xkmc.l2weaponry.init;

/**
 * 仅用于 Mixin 注解处理器校验注入目标：给 {@code L2WeaponryConstructMixin} 的
 * {@code @Mixin(targets = "...")} 提供一个同名类。
 *
 * <p>不会被编译进本模组的 jar，也不会在运行时出现（sourceSet 是 stubs，只进 compileOnly 类路径）。
 * 真正的类来自玩家的 l2weaponry。</p>
 */
public class L2Weaponry {
    public L2Weaponry() {
    }
}

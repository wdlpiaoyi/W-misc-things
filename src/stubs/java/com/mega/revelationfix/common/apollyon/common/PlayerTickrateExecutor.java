package com.mega.revelationfix.common.apollyon.common;

/**
 * 仅用于给 Mixin handler 提供参数类型（编译期）：RevelationFix 的
 * {@code PlayerTickrateExecutor#processInventory(Player, InventoryWork)} 的第二个参数。
 *
 * <p>只进 compileOnly，不进 jar；运行时真正提供这个类的是内嵌的 RevelationFix。</p>
 */
public final class PlayerTickrateExecutor {
    private PlayerTickrateExecutor() {
    }

    public static final class InventoryWork {
        private InventoryWork() {
        }
    }
}

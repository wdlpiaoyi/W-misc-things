# 开发环境

## JDK

Minecraft 1.20.1 / ForgeGradle 6 只认 **Java 17**：

```
C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot
```

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot'
.\gradlew.bat build             # 出 jar
.\gradlew.bat runClient         # 开发跑客户端（开发环境没有第三方模组，mixin 会按前置缺失跳过）
.\gradlew.bat runGameTestServer # 冒烟：加载完模组、跑完 Gametest 后自动退出
.\gradlew.bat --offline build   # 依赖全在本地缓存时用
```

## Gradle

- wrapper：Gradle 8.8（走华为云镜像）
- ForgeGradle 6 不支持 configuration cache，别开
- 不要提交 `/.gradle/`、`/build/`、`/run/`

## 版本与依赖

| 项 | 值 |
|---|---|
| Minecraft | 1.20.1 |
| 编译基线 Forge | 1.20.1-47.4.10（`gradle.properties: forge_version`） |
| 目标运行环境 | Forge 47.4.28 |
| mappings | `official` 1.20.1 |
| Java | 17（构建） |

`compileOnly fg.deobf('top.theillusivec4.curios:curios-forge:5.14.1+1.20.1')`：
Apollyon 饰品归还用到 curios 槽位 API，仓库是 `https://maven.theillusivec4.top/`。

spear-core 没有可用 maven 坐标，所以 `net.minecraft.spearcore.item.SpearLike` 只在
`src/stubs/java` 放了个空接口（只进 `compileOnly`，不进 jar），运行期由实例里的 spear-core 提供。
真实类型只出现在 `MowzieSpearLikeSupportMixin` 的泛型参数与转型里，方法调用全走
`SpearCoreBridge` 的反射，所以空接口够用。

## 游戏内命令

| 命令 | 作用 |
|---|---|
| `/wmisc reload` | 重读 `config/w_misc-common.toml`，把值同步给各模块（不重建 ForgeConfigSpec） |
| `/wmisc wtc_resource_hud` | 打开 WTC 资源条位置编辑界面（拖拽/滚轮/方向键） |

两者都注册在 `RegisterClientCommandsEvent` 上，所以是客户端命令（不需要 OP，联机也能用）。
新增可热重载的选项：模块实现 `WmiscFeature#onConfigReload(Map)`，用 `ConfigPeek.applyBoolean/applyInt/applyDouble`
把值写回自己的字段。

## Mixin 约定（踩过的坑）

1. **目标类不在编译期类路径**时，用 `@Mixin(targets = "全名", remap = false)` + `@Pseudo`，
   不要 import 目标类。`@Pseudo` 还能让注解处理器跳过目标校验（否则要造 stub 类）。
2. **不要在构造函数里注入前置逻辑**：Mixin 0.8.5 对构造函数只允许 TAIL 注入；HEAD 报
   "before super()"，INVOKE 注入点报 "Found @Inject targetting a constructor"。
   要锁「构造期危险区」，就注入它调用的入口方法（例如 `IceAndFire.init()` / `LWItems.register()`）
   的 HEAD/RETURN。
3. **不要试图注入 `FMLModContainer#constructMod`**：那在 ModLauncher 启动层，Mixin 只处理游戏层，
   注入不会被应用（实测无任何日志、`.mixin.out` 为空）。要动构造时序就注入被注入模组自己的主类。
4. 需要参数类型（例如 `PlayerTickrateExecutor$InventoryWork`）而目标类不在 classpath 时，
   在 `src/stubs/java` 放同名空壳类，只进 `compileOnly`，**不要进 jar**。
5. 跨模组类的方法名放进 refmap 会坏，mod 自有方法一律 `remap = false`；可选目标再加 `require = 0`。
6. **要改被注入类本身（不只是注入方法）时，用 `IMixinConfigPlugin#preApply` 直接改 `ClassNode`**：
   `L2ModularBlockFinalPatch` 就是这么摘 l2modularblock 的 `ACC_FINAL` 的，不受目标方法名/描述符影响；
   改写失败只记日志，别抛异常拦启动。
7. **同一条指令上的 `@Redirect` 冲突**：Mixin 0.8.5 一条指令只允许一个 redirect。prepare 阶段按
   mixin 优先级升序处理，后到的如果 priority 不高于先到的，直接被跳过并打
   `@Redirect conflict. Skipping ...`。**被跳过不等于没事**：如果对方那条是 `require = 1`
   （carbon 的 `imo$foil` 就是），它会抛 `InjectionError: (0/1) succeeded` 直接崩档；而且这类崩
   发生在 mod 构造**之前**（Bootstrap 阶段加载 `ItemRenderer` 就触发），mod 构造期再动手已经来不及。
   两种处理方式：
   - 双方都有 `require = 0`、且你愿意接管语义 → 用 `@Mixin(priority = 1500)` 抢，注意被抢走的
     一方会静默失效，必须把它的逻辑合并进你的 handler；
   - 对方 `require = 1` / 不想赌 priority → **别抢**，反过来在自己配置的
     `IMixinConfigPlugin#acceptTargets`（此时所有配置已 prepare、还没有类被应用）里用
     `compat/mixin/MixinDetacher` 把对方那条 mixin 摘掉，再复刻/合并它的其余注入
     （`carbon_saintsdragons_foil` 就是这么做的）。
   可选开关用 `IMixinConfigPlugin#shouldApplyMixin` + `ConfigPeek` 在启动前判定（运行期热改无效）。

## 实例侧验证

两个 1.20.1 实例：`C:\Minecraft\PrismLauncher-Portable\instances\Niubiosis` 与 `1.20.1-forge`。

```powershell
# 拷贝产物
Copy-Item .\build\libs\w_misc-1.20.1-forge-0.2.2.jar 'C:\Minecraft\PrismLauncher-Portable\instances\Niubiosis\minecraft\mods\' -Force
# 启动并看日志
Start-Process 'C:\Minecraft\PrismLauncher-Portable\prismlauncher.exe' -ArgumentList '-d','C:\Minecraft\PrismLauncher-Portable','-l','Niubiosis','-o','测试名'
```

判定标准（Niubiosis，`config/fml.toml` 的 `maxThreads` 必须是 -1，否则测不出死锁修复）：

- 日志出现 `[deadlock_fix] 已接管 l2weaponry 的构造阶段` 与 `已接管 iceandfire 的构造阶段`，
  两条相隔一段时间而不是同一瞬间；
- 继续出现 `[w_misc] 死锁修复已生效：…`；
- 最终到 `title-screen-operable`，没有 `waiting on the Class initialization monitor`。

内容过滤不依赖实例侧文件：规则在 `compat/content/ContentFilterRules`，由 `ResourceManagerFilterMixin`
在资源管理器返回资源后改写。改动规则后重进游戏即可，不需要重新摆资源包。



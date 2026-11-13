# w_misc —— wdlpiaoyi's Miscellaneous Things

Minecraft **1.20.1** / **Forge 47.x** 的杂项复合模组：若干互相独立的小功能、兼容修补和跨模组联动，集中在一个模组里，逐项可开关。

- modId：`w_misc`
- 目标运行环境：Minecraft 1.20.1 + Forge 47.4.28（编译基线 Forge 47.4.10，同属 47.x）
- 配置：`config/w_misc-common.toml`，`[general]` + 每个模块一段 `[features.<id>]`
- 前置模组缺失时，对应模块自动跳过，模组本身照常启动

## 功能模块

| 模块 id | 作用 | 前置 | 状态 |
|---|---|---|---|
| `deadlock_fix` | 串行化 `iceandfire` 与 `l2weaponry` 的构造阶段，消除 JVM 类初始化锁环（启动假死） | iceandfire、l2weaponry | 已实测生效 |
| `fantasy_ending_upgrade_orb` | 补 `fantasy_ending:fantasy_power` 的 `upgrade_orb_type` 数据条目 | irons_spellbooks、fantasy_ending | 已随包提供 |
| `chloride_jei_bundle` | 并入 chloride 1.8.1 / JEI 15.70+ 兼容补丁（替代独立 jar） | chloride、jei | 已实测生效 |
| `goety_apollyon_curio_return` | Apollyon 战后水晶把饰品按原槽位装回，而不是一件件掉到脚下 | revelationfix、curios | 待实机战斗验证 |
| `enigmatic_legacy_creative` | 创造模式下无视七咒之戒的佩戴时长条件 | enigmaticlegacy（含 addons） | 待实机验证 |
| `content_filter` | 屏蔽模组注入的政治宣传内容 | — | 部分随包 + 实例侧数据/资源 |

启动日志会打印模块状态表，并自检修复是否真的生效（例如死锁修复会确认两个模组的构造入口都被接管）。

## 各处实现要点

**死锁修复**：锁加在两个模组真正触碰 Iaf* 类的入口方法上——
`com.iafenvoy.iceandfire.IceAndFire#init()` 与 `dev.xkmc.l2weaponry.init.registrate.LWItems#register()`，
HEAD 取同一把共享锁、RETURN 释放。
不能用 `FMLModContainer#constructMod`（ModLauncher 启动层，Mixin 覆盖不到，已实测）；
也不能注入构造函数（Mixin 0.8.5 在构造函数里只允许 TAIL 注入，已实测）。

**fantasy_ending 数据补丁**：纯资源，`data/fantasy_ending/irons_spellbooks/upgrade_orb_type/fantasy_power.json`，
值取自 fantasy_ending 原本的代码注册（0.05 / `fantasy_ending:fantasy_spell_power` / `MULTIPLY_BASE`）。
进世界时校验注册表里确实查得到这个键。

**chloride 兼容**：纯反射从 Mixin 运行时摘掉 chloride 那条失效的 `jei_rei_emi.JeiOverlayMixin`，
不改 jar，版本门控只对 chloride 1.8.1 + JEI ≥ 15.70.0.221 生效。
装本模组后**删掉实例里原来的 `chloridejeicompat-*.jar`**。

**Apollyon 饰品归还**：`processInventory` HEAD 快照（curio 槽位类型 + index + 盔甲槽），存玩家 ForgeData；
右键水晶时拦截 `RightClickBlock`，按快照装回；槽位提供者先装并手动 `addTransientSlotModifiers`，
否则同一 tick 里额外槽位还没生效。水晶里原有物品全部取出，溢出按配置掉地。

**七咒之戒**：`SuperpositionHandler#isTheWorthyOne(Player)` RETURN 注入，创造模式直接 true；
与附属 EnigmaticAddons 的同点 RETURN 注入叠加安全，不伪造计数（不影响 BlessRing 等级）。

**内容过滤**（全部在模组内部，换实例不用另外放资源包/数据包）：
- 资源层：`ResourceManagerFilterMixin` 在 `MultiPackResourceManager` 返回资源之后按规则改写——
  Malum prideweave 的配方/进度替换成不可达占位、pride 贴图替换成 1x1 透明、Malum/IE/Mekanism 语言文件
  里的 pride 条目改成中性文本、主菜单 splash 按行过滤宣传句。与模组之间资源包顺序无关。
- Quark：彩虹 Q 按钮由 `QuarkQButtonMixin` 固定前景色；13 种 LGBT 蜜蜂每 20 tick 用反射把
  `enableLGBTBees / everyBeeIsLGBT` 压回 false（配置界面改回来也会被压回去）。
- 规则集中在 `compat/content/ContentFilterRules`，一行一条，路径正则 + 处理类型。

## 结构

```
src/main/java/com/wdlpiaoyi/wmisc/
  Wmisc.java               入口：配置 + 构造期/common/client 钩子
  config/WmiscConfig.java  配置定义（[general] + [features.*]）
  feature/                 模块接口、登记表、模块清单
    impl/                  各模块实现
  compat/                  跨模组桥接（deadlock / chloride / spellbooks / apollyon）
  mixin/                   Mixin 注入与按前置模组软开关的插件
  util/                    ConfigPeek：配置加载前的配置文件读取
src/stubs/java/            仅供 Mixin 注解处理器/参数类型使用的空壳类，不进 jar
src/main/resources/
  META-INF/mods.toml
  w_misc.mixins.json
  data/fantasy_ending/...  数据包补丁（upgrade_orb_type）
  data/malum/...           prideweave 屏蔽
  assets/minecraft/texts/splashes.txt
```

**加一个功能**：在 `feature/impl` 下实现 `WmiscFeature`，在 `Features.init()` 里 `register` 一行。

## 构建

需要 **JDK 17**（1.20.1 必须；JDK 21 / 25 跑客户端可以，但构建用 17）：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot'
.\gradlew.bat build
```

产物：`build/libs/w_misc-1.20.1-forge-0.1.0.jar`（`reobfJar` 已固定在 `jar` 之后，勿去掉）。

## 安装

把产物 jar 放进 Prism 实例的 `mods/`。首次启动会在 `config/` 下生成 `w_misc-common.toml`。

## 备注

`todo.md`（本地待办）已在 `.gitignore` 里排除，不入库；`notes/` 是本机分析笔记，同样不入库。



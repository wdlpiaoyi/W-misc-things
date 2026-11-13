# w_misc —— wdlpiaoyi's Miscellaneous Things

Minecraft **1.20.1** / **Forge 47.x** 的杂项复合模组：若干互相独立的小功能、兼容修补和跨模组联动，集中在一个模组里，逐项可开关。

- modId：`w_misc`
- 目标运行环境：Minecraft 1.20.1 + Forge 47.4.28（编译基线 Forge 47.4.10，同属 47.x）
- 配置：`config/w_misc-common.toml`，`[general]` + 每个模块一段 `[features.<id>]`
- 前置模组缺失时，对应模块自动跳过，模组本身照常启动

## 功能模块

| 模块 id | 作用 | 前置 |
|---|---|---|
| `deadlock_fix` | 串行化 `iceandfire` 与 `l2weaponry` 的构造阶段，消除 JVM 类初始化锁环（启动假死） | iceandfire、l2weaponry |
| `fantasy_ending_upgrade_orb` | 补 `fantasy_ending:fantasy_power` 的 `upgrade_orb_type` 数据条目 | irons_spellbooks、fantasy_ending |
| `chloride_jei_bundle` | 并入 chloride 1.8.1 / JEI 15.70+ 兼容补丁（替代独立 jar） | chloride、jei |
| `goety_apollyon_curio_return` | Apollyon 战后饰品按原槽位一键归还 | goety_revelation、curios |
| `enigmatic_legacy_creative` | 创造模式下忽略七咒之戒的佩戴时长条件 | enigmatic legacy（+ addons） |
| `content_filter` | 按配置规则过滤模组注入的内容 | — |

启动日志会打印一张模块状态表（启用 / 配置停用 / 缺少前置 / 实现待补），并自检修复是否真的生效。

## 几个实现要点

**死锁修复**：注入的是两个 mod 的**主类构造函数**（`com.iafenvoy.iceandfire.forge.IceAndFireForge`、
`dev.xkmc.l2weaponry.init.L2Weaponry`），HEAD 取同一把共享锁、RETURN 释放。
不能注入 `FMLModContainer#constructMod`：那个类在 ModLauncher 启动层，Mixin 只处理游戏层，已实测不生效。

**fantasy_ending 数据补丁**：纯资源，`data/fantasy_ending/irons_spellbooks/upgrade_orb_type/fantasy_power.json`，
值取自 fantasy_ending 原本的代码注册（0.05 / `fantasy_ending:fantasy_spell_power` / `MULTIPLY_BASE`）。
进世界时会校验注册表里确实查得到这个键，缺了会报 ERROR。

**chloride 兼容**：纯反射从 Mixin 运行时里摘掉 chloride 那条失效的 `jei_rei_emi.JeiOverlayMixin`，
不改 chloride / JEI 的 jar，版本门控只对 chloride 1.8.1 + JEI ≥ 15.70.0.221 生效。
装本模组后**必须删掉实例里原来的 `chloridejeicompat-*.jar`**，否则两个模组会各摘一次（幂等但没必要）。

## 结构

```
src/main/java/com/wdlpiaoyi/wmisc/
  Wmisc.java               入口：配置 + 构造期/common/client 钩子
  config/WmiscConfig.java  配置定义（[general] + [features.*]）
  feature/                 模块接口、登记表、模块清单
    impl/                  各模块实现
  compat/                  跨模组探测与桥接（deadlock / chloride / spellbooks）
  mixin/                   Mixin 注入与按前置模组软开关的插件
  util/                    ConfigPeek：配置加载前的配置文件读取
src/stubs/java/            仅供 Mixin 注解处理器校验的空壳目标类，不进 jar
src/main/resources/
  META-INF/mods.toml
  w_misc.mixins.json
  data/fantasy_ending/...  数据包补丁
  pack.mcmeta
```

**加一个功能**：在 `feature/impl` 下实现 `WmiscFeature`，在 `Features.init()` 里 `register` 一行。
配置段、状态日志、前置判定都会自动生效。

## 构建

需要 **JDK 17**（1.20.1 必须；JDK 21 / 25 不行）：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot'
.\gradlew.bat build
```

产物：`build/libs/w_misc-1.20.1-forge-0.1.0.jar`（`reobfJar` 已固定在 `jar` 之后，勿去掉）。

Gradle 8.8，wrapper 走华为云镜像；ForgeGradle 6 不支持 configuration cache。

## 安装

把产物 jar 放进 Prism 实例的 `mods/`。首次启动会在 `config/` 下生成 `w_misc-common.toml`。

## 备注

`todo.md` 与 `死锁修复技术方案.md` 是本地工作笔记，已在 `.gitignore` 里排除，不入库。

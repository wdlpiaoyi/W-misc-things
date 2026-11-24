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
| `l2modularblock_final_fix` | 摘掉 l2modularblock 新版加上的 `final` 标记，救回按旧版编译的附属（如 arsdelight） | — | 已实现 |
| `fantasy_ending_upgrade_orb` | 补 `fantasy_ending:fantasy_power` 的 `upgrade_orb_type` 数据条目 | irons_spellbooks、fantasy_ending | 已随包提供 |
| `chloride_jei_bundle` | 并入 chloride 1.8.1 / JEI 15.70+ 兼容补丁（替代独立 jar） | chloride、jei | 已实测生效 |
| `irons_arcane_anvil_jei` | 裁掉奥术铁砧 JEI 配方的组合爆炸（配方条数与每槽候选设上限） | irons_spellbooks、jei | 已实现 |
| `goety_apollyon_curio_return` | Apollyon 战后水晶把饰品按原槽位装回，而不是一件件掉到脚下 | revelationfix、curios | 待实机战斗验证 |
| `enigmatic_legacy_creative` | 创造模式下无视七咒之戒的佩戴时长条件 | enigmaticlegacy（含 addons） | 待实机验证 |
| `enigmatic_soul_recovery` | 每 N tick（默认 6000 = 5 分钟）自动回复一点丢失的灵魂水晶 | enigmaticlegacy | 已实现 |
| `goetic_ggtu_dimension` | 个人/心灵维度里暂停 GGTU 的帧生成与超分：修 skybox/边界墙闪烁与超分穿透 | goeticlegacy、resolutionscaler | 已实现 |
| `spearcore_mowzie_spear` | 让 `mowziesmobs:spear` 走 SpearCore 的长矛逻辑（戳刺、蓄力、动画、攻击力） | mowziesmobs、spearcore | 已实现 |
| `mowzie_sculptor_no_anticheat` | 去掉雕塑家试炼的反作弊判定，并在悬停提示里说明 | mowziesmobs | 已实现 |
| `wtc_resource_hud` | WTC 肾上腺素/怒气条的显隐条件 + `/wmisc wtc_resource_hud` 位置编辑 | wizard_terra_cuiros | 已实现 |
| `no_narrator` | 彻底关掉旁白：不加载 TTS 底层库、吞掉所有发声入口、Ctrl+B 失效 | — | 已实现 |
| `carbon_saintsdragons_foil` | 修 carbon × Saint's Dragons 在 `ItemRenderer#render` 里 `hasFoil` 的 `@Redirect` 冲突（原本直接崩档）：`hasFoil` 让给 carbon 独占，摘掉圣龙那条 mixin 并复刻其其余注入 | carbon、saintsdragons | 已实测能启动 |
| `apprenticecodex_saintsdragons_glint` | 在合并后的 `getFoilBuffer*` 重定向里恢复 Apprentice's Codex 的绑定武器自定义附魔光 | apprenticecodex、saintsdragons | 已实现 |
| `content_filter` | 屏蔽模组注入的政治宣传内容 | — | 部分随包 + 实例侧数据/资源 |

启动日志会打印模块状态表，并自检修复是否真的生效（例如死锁修复会确认两个模组的构造入口都被接管）。

游戏内命令（客户端命令，单人/联机都可用）：

- `/wmisc reload`：重读 `config/w_misc-common.toml` 并立刻生效（不重启、不重建 ForgeConfigSpec）
- `/wmisc wtc_resource_hud`：打开 WTC 资源条位置编辑界面（拖拽移动、滚轮缩放、方向键微调）

## 加载期兼容补丁（没有模块开关）

| 补丁 | 作用 | 前置 |
|---|---|---|
| `FantasyGeckolibAnimTimeMixin` | 往 geckolib `GeoModel` 补一个 `animTime` 占位字段，让 fantasy_ending 的 `@Accessor` 能加载（代价：它的动画时间逻辑静默失效） | fantasy_ending、geckolib |

## 结构

```
src/main/java/com/wdlpiaoyi/wmisc/
  Wmisc.java               入口：配置 + 构造期/common/client 钩子
  config/WmiscConfig.java  配置定义（[general] + [features.*]）
  feature/                 模块接口、登记表、模块清单
    impl/                  各模块实现
  compat/                  跨模组桥接
    deadlock/              iceandfire × l2weaponry 串行化
    chloride/              反射摘 Mixin + JEI 补丁
    l2modularblock/        改写目标 ClassNode 摘 final
    irons/                 奥术铁砧 JEI 裁剪（含两个 @Mixin 接口）
    spellbooks/            upgrade_orb_type 注册校验
    apollyon/              Apollyon 饰品快照与归还
    enigmatic/             灵魂水晶回复
    goetic/                goetic 维度判定
    spearcore/             SpearCore 长矛桥接（+ client/ 手持变换）
    mowzie/                雕塑家试炼兼容
    wtc/                   Wizard Terra Curios 资源条反射桥与编辑界面
    content/               内容过滤规则表
    WmiscCommands.java     /wmisc 客户端命令
  mixin/                   Mixin 注入与按前置模组软开关的插件
  util/                    ConfigPeek：配置加载前的配置文件读取 + 热重载
src/stubs/java/            仅供编译期使用的空壳类（revelationfix、spearcore 的 SpearLike），不进 jar
src/main/resources/
  META-INF/mods.toml
  w_misc.mixins.json
  data/fantasy_ending/...  数据包补丁（upgrade_orb_type）
  assets/w_misc/lang/      命令与界面的中英文文本
  logo.png / pack.mcmeta
```

**加一个功能**：在 `feature/impl` 下实现 `WmiscFeature`，在 `Features.init()` 里 `register` 一行；
要前置缺失时自动跳过就实现 `requiredMods()`，要在 `[features.<id>]` 里加选项就实现 `defineOptions()`，
要支持 `/wmisc reload` 就实现 `onConfigReload()`。

## 构建

需要 **JDK 17**（1.20.1 必须；JDK 21 / 25 跑客户端可以，但构建用 17）：

```powershell
$env:JAVA_HOME = '<JDK 17 的安装目录>'
.\gradlew.bat build
```

产物：`build/libs/w_misc-1.20.1-forge-0.2.2.jar`（`reobfJar` 已固定在 `jar` 之后，勿去掉）。

第三方 mod 的编译期依赖尽量不放仓库：spear-core / revelationfix 这类只在少数 mixin 里用到的类，
在 `src/stubs/java` 放同名空壳类即可（只进 `compileOnly`，不进 jar，运行时由真实模组提供）。

## 安装

把产物 jar 放进对应 Minecraft 1.20.1 实例的 `mods/`。首次启动会在 `config/` 下生成
`w_misc-common.toml`；不需要额外的资源包或数据包，卸载时删掉 jar 和这个配置文件即可。

## 备注

- 所有跨模组补丁都是纯 Mixin / 反射实现，不改第三方 jar；前置缺失或反射失败时对应模块自动跳过。
- 启用 `chloride_jei_bundle` 时，请删掉实例里原来的 `chloridejeicompat-*.jar`（功能已并入本模组）。

## 文档

- [docs/implementation.md](docs/implementation.md)：各模块实现要点与踩过的坑
- [docs/development.md](docs/development.md)：开发环境、构建与 Mixin 约定

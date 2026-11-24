# 各模块实现要点

本文件记录每个模块/补丁「为什么这么做」和踩过的坑；模块清单与开关见 [README](../README.md)。
开发环境、Mixin 通用约定见 [development.md](development.md)。

## deadlock_fix（死锁修复）

锁加在两个模组真正触碰 Iaf* 类的入口方法上——
`com.iafenvoy.iceandfire.IceAndFire#init()` 与 `dev.xkmc.l2weaponry.init.registrate.LWItems#register()`，
HEAD 取同一把共享锁、RETURN 释放。锁本身用 `tryLock(60s)` 起手：超时就打一条 warn 再无限排队，
避免另一侧真的卡死时日志里什么都看不到。

不能用 `FMLModContainer#constructMod`（ModLauncher 启动层，Mixin 覆盖不到，已实测）；
也不能注入构造函数（Mixin 0.8.5 在构造函数里只允许 TAIL 注入，已实测）。

## fantasy_ending_upgrade_orb（数据补丁）

纯资源，`data/fantasy_ending/irons_spellbooks/upgrade_orb_type/fantasy_power.json`，
值取自 fantasy_ending 原本的代码注册（0.05 / `fantasy_ending:fantasy_spell_power` / `MULTIPLY_BASE`）。
进世界时校验注册表里确实查得到这个键。

**坑：数据包不认识“模组没装”**。数据包是按命名空间合并加载的，只要本模组在，
`data/fantasy_ending/...` 就会进数据包——哪怕 fantasy_ending 根本不在 mod 列表里。此时条目引用的
`fantasy_ending:fantasy_spell_power`（属性）和 `fantasy_ending:fantasy_upgrade_orb`（物品）都不存在，
irons_spellbooks 加载 `irons_spellbooks:upgrade_orb_type` 注册表时会报
`Failed to parse fantasy_ending:irons_spellbooks/upgrade_orb_type/fantasy_power.json from pack w_misc-*.jar`，
接着 `RegistryDataLoader` 抛 “Failed to load registries due to above errors”，进世界直接崩。

修法：`ConditionalResources` + `ResourceManagerFilterMixin` 在资源管理器这一层把它藏起来。
判定用 `FeatureRegistry.liveActive("fantasy_ending_upgrade_orb")`——配置开关关掉、或
irons_spellbooks / fantasy_ending 任一缺失时，`listResources` / `getResource` 里就看不到这个 id，
数据包注册表自然也不会去解析它；前置齐全时行为与以前完全一样。放在资源管理器层而不是删 jar 里的文件，
是因为同一个 jar 要同时喂给两个实例（一个装了 fantasy_ending、一个没装）。

## chloride_jei_bundle（chloride 兼容）

纯反射从 Mixin 运行时摘掉 chloride 那条失效的 `jei_rei_emi.JeiOverlayMixin`，
不改 jar，版本门控只对 chloride 1.8.1 + JEI ≥ 15.70.0.221 生效。
装本模组后**删掉实例里原来的 `chloridejeicompat-*.jar`**。

## goety_apollyon_curio_return（Apollyon 饰品归还）

`processInventory` HEAD 快照（curio 槽位类型 + index + 盔甲槽），存玩家 ForgeData；
右键水晶时拦截 `RightClickBlock`，按快照装回；槽位提供者先装并手动 `addTransientSlotModifiers`，
否则同一 tick 里额外槽位还没生效。水晶里原有物品全部取出，溢出按配置掉地。

## enigmatic_legacy_creative / enigmatic_soul_recovery

**七咒之戒**：`SuperpositionHandler#isTheWorthyOne(Player)` RETURN 注入，创造模式直接 true；
与附属 EnigmaticAddons 的同点 RETURN 注入叠加安全，不伪造计数（不影响 BlessRing 等级）。

**灵魂水晶回复**：`SoulCrystalRecovery` 反射调 enigmaticlegacy 的 `getLostCrystals / setLostCrystals`，
每 `interval_ticks` 补 `amount` 点；反射失败只记一次日志，不刷屏、不影响其它模块。

## l2modularblock_final_fix

`L2ModularBlockFinalPatch` 在 `WmiscMixinPlugin#preApply` 里直接改写目标 `ClassNode`，
把方法上的 `ACC_FINAL` 摘掉——不是注入，所以不受目标方法名/描述符影响；
只对登记过的类生效，失败只记日志，不拦启动。

## irons_arcane_anvil_jei

`AnvilJeiBudget` 在 JEI 拿到配方列表后按 `max_imbue_recipes` / `max_items_per_slot` 等间隔抽样裁剪
（保序、成对裁），`AnvilRecipeView` / `AnvilTupleView` 是给 `@Mixin` 加的接口（`wmisc$leftItem` 等），
用来在配对上取原始物品做分组。

## goetic_ggtu_dimension

`GoeticDimensionGate` 判断玩家是否在 goeticlegacy 的个人/心灵维度，
`GgtuGoeticDimensionMixin` 让 GGTU 跳过该维度的帧生成与超分辨率（代价是该维度没有 GGTU）。

## spearcore_mowzie_spear

`SpearCoreBridge` 全反射取 spear-core 的 `SpearLike` / `SpearLikeSupport` / 伤害事件，
`MowzieSpearCompat` 用 `Proxy` 造一个 `SpearLike` 代理塞回 spear-core；
`MowzieSpearItemMixin` 把 `Item#use / onUseTick / getUseDuration` 掐进蓄力逻辑，
`MowzieSpearClientSetupMixin` / `MowzieSpearReequipMixin` 补客户端手持变换与重新装备。
前置缺失或反射失败时整个模块静默跳过，原版木矛行为不受影响。

**踩坑：动态事件不能直接注册**。Forge 的 eventbus（6.0 / 6.2 都一样）在注册监听器时，
对非抽象事件类会先 `getConstructor()` 造一个事件对象、再取它的 `ListenerList`
（`EventListenerHelper.computeListenerList`）。spear-core 的 `SpearDamageEvent` 只有带参构造，
于是注册直接抛 `NoSuchMethodException`（包在 `RuntimeException` 里），把整个模块的构造期初始化带崩。

修法：注册前先用反射调 `EventListenerHelper.getListenerListInternal(SpearDamageEvent, true)`
（也就是「事件实例自己查表」那条路径），把 `ListenerList` 预建进缓存——事件 POST 时查到的正是同一个对象，
之后注册就不需要再实例化事件。注册本身仍包在 try/catch 里：万一下个版本内部接口变了，
只损失「秒杀小动物」这一个效果，蓄力/动画/攻击力照常。

## mowzie_sculptor_no_anticheat

`SculptorTestCheatMixin` 让雕塑家试炼的反作弊判定直接放行（放/破方块、传送、飞行、游泳、向上加速、
离开场地都不再算失败），`SculptorTradeHoverMixin` 在悬停提示里说明这一点。
判定入口拿不到时只记日志，不拦启动。

## wtc_resource_hud

`WtcHud` 反射拿 Wizard Terra Curios 的条位置/缩放配置，`WtcHudScreen` 提供拖拽/滚轮/方向键编辑
并写回它的配置；`WtcBarVisibility` 只管显隐条件（空值/死亡/旁观/久无变化）。
没接上 WTC 的配置时位置编辑不可用，但显隐条件仍然生效；`/wmisc wtc_resource_hud` 打不开时会给出提示。

## no_narrator

`NoNarratorLibraryMixin` 拦 TTS 底层库加载，`NoNarratorFunctionsMixin` 吞掉所有发声入口，
`NoNarratorHotkeyMixin` 让 Ctrl+B 失效；进游戏时再把「旁白」选项压成 OFF。
四项各有开关（`block_library` / `disable_speech` / `block_hotkey` / `force_option_off`），
其中 `block_library` 在 mod 构造期就要读，所以走 `ConfigPeek`。

## content_filter（内容过滤）

全部在模组内部，换实例不用另外放资源包/数据包。

- 注册层：Malum 的 18 个 prideweave **直接不注册**（`MalumPrideItemRemovalMixin` 拦在
  `ItemRegistry.register`，返回 air 的 RegistryObject；引用它们的 `ArmorSkinRegistry.registerPridewear`
  由 `MalumPrideArmorSkinMixin` 跳过）→ JEI/创造栏/`/give` 里都不存在；
  Immersive Engineering 的 6 个 pride 着色器在 `IEShaders.commonConstruction()` 之后从
  `ShaderRegistry` / 战利品列表摘除（`ImmersiveEngineeringShaderMixin`）。
- 资源层：`ResourceManagerFilterMixin` 在 `MultiPackResourceManager` 返回资源之后按规则**删除或改写**——
  Malum 的 prideweave 配方/进度、Mekanism 的 11 个 pride robit 皮肤数据整条删除（不会进入注册表）；
  pride 贴图替换成 1x1 透明；Malum/IE/Mekanism/Quark 语言里的 pride 条目改成中性文本；
  Quark 的彩虹隐藏进度删除；主菜单 splash 按行过滤宣传句。与模组之间资源包顺序无关。
- Quark：彩虹 Q 按钮由 `QuarkQButtonMixin` 固定前景色；13 种 LGBT 蜜蜂每 20 tick 用反射把
  `enableLGBTBees / everyBeeIsLGBT` 压回 false（配置界面改回来也会被压回去）。
- 规则集中在 `compat/content/ContentFilterRules`，一行一条，路径正则 + 处理类型。

## carbon_saintsdragons_foil

carbon 的 `first.carbon.mixin.ItemRendererMixin#imo$foil`（priority 1000）和 Saint's Dragons 的
`client.BloodTempestAfterimageItemRendererMixin#saintsdragons$hideAfterimageFoil`（priority 1000）
都在 `ItemRenderer#render` 里 `@Redirect` 同一条 `ItemStack#hasFoil()` 指令（`render` 里一共 3 处调用：
动画贴图分支 + `getFoilBufferDirect` / `getFoilBuffer` 的实参）。Mixin 0.8.5 一条指令只允许一个
redirect：prepare 按优先级升序处理，后到且优先级不更高的直接被跳过。

**这条冲突会让游戏起不来**（不是静默降级）：carbon 的 `imo$foil` 要求 `require = 1`，它的 3 条被跳过 →
`InjectionError: Critical injection failure: Redirector imo$foil ... (0/1) succeeded` → 进程退出码 2。
而且它崩在 mod 构造**之前**（`Bootstrap` 阶段 `RenderType.<clinit>` 触发 `ItemRenderer` 变换），
所以任何「mod 构造期再摘 mixin」的做法都来不及。

因此本模块**不抢**这条指令，反过来：

1. `WmiscMixinPlugin#acceptTargets`（所有配置已 prepare、尚无类被应用 mixin 的钩子）调用
   `compat/mixin/MixinDetacher`，把圣龙的 `client.BloodTempestAfterimageItemRendererMixin`
   从 `saintsdragons.mixins.json` 里摘掉（纯反射，不改第三方 jar）；
2. `hasFoil` 那条留给 carbon **独占**，所以它永远注入成功、`require` 永远满足；
3. 圣龙被摘掉的另外三条注入由 `ItemRendererAfterimageRedirectMixin`（`priority = 1500`）复刻：
   - `BakedModel#getRenderTypes` → 残影期间换 `Sheets.translucentItemSheet()`；
   - `ItemRenderer#hasAnimatedTexture` → 残影期间返回 false（绕开罗盘/时钟那套放大 + 附魔光）；
   - `getFoilBuffer` / `getFoilBufferDirect` → 残影期间把 `glint` 实参压成 false（等效圣龙原来把
     `hasFoil` 压成 false 的效果），并套它的染色 consumer。

   这三条复刻在 `compat/foil/ItemRendererOverrides` 里；圣龙的状态由
   `compat/saintsdragons/BloodTempestAfterimage`（MethodHandle 反射桥）读。

这样 carbon 的掉落物阴影捕获语义（`DROPPED_RENDERER.capturingModel()` 时 `hasFoil=false`）和圣龙的
残影表现都保留，唯一差别是「残影期间罗盘/时钟」这类组合——`hasAnimatedTexture` 那条复刻把它还原成
和原版一致，因此实际上没有可观察差异。

开关只在启动前有效：摘除与 mixin 都在 `ItemRenderer` 类加载期完成，运行期热改配置无效。启动日志里
会出现 `[carbon_saintsdragons_foil] 摘除 saintsdragons 的 ItemRenderer mixin：REMOVED` 与
`ItemRendererAfterimageRedirectMixin 已合入 ItemRenderer（priority 1500）` 两条自证；`saintsdragons`
那条 `@Redirect conflict` 不再出现（它的 mixin 已经不在了），而 carbon 的 `imo$foil` 不再被跳过。

## apprenticecodex_saintsdragons_glint

同一类问题：Apprentice's Codex 的 `ItemRendererBoundWeaponGlintMixin`（priority 900）在 `render` 里
redirect `getFoilBuffer` / `getFoilBufferDirect`（handler 额外捕获 `render` 的全部实参），给绑定武器
（`BoundBowItem#isBoundBow` / `BoundSwordItem#isBoundSword`）额外写一张自定义 glint 贴图
（`ApprenticeRenderTypes#boundSpellWeaponGlint(Direct)`）。这两条指令原本被圣龙残影（1000）压着，
Codex 一直静默失效。它自己的注解带 `require = 0`，所以就算再被跳过也不会崩。

现在 `ItemRendererAfterimageRedirectMixin`（priority 1500）统一持有这两条指令，合并逻辑在
`ItemRendererOverrides.foilBuffer`：先按 Codex 规则加 glint 缓冲，再交给残影 consumer 包裹。
本模块的开关（运行期可热改）决定是否在合并里加回 Codex 那部分；它依赖
`carbon_saintsdragons_foil` 在位（那条负责类加载期的接管）。

## 加载期兼容补丁（没有模块开关）

### fantasy_ending × geckolib 4.8+：`animTime` 占位字段

fantasy_ending 2.7.x（内部包名 `com.mega.uom`）的 `com.mega.uom.mixin.geckolib.GeoModelAccessor`
用 `@Accessor("animTime")` 读写 geckolib `GeoModel#animTime`（double）。geckolib 4.8 删掉了这个字段，
而 uom 的 mixin 配置是 `required=true`，accessor 在 GeoModel 类加载时找不到字段就会抛
InvalidMixinException，把启动打断。

`FantasyGeckolibAnimTimeMixin` 只做一件事：把同名同类型的 `double animTime` 字段塞回 `GeoModel`，
让 accessor 能解析。这个字段没有任何写入点，所以 fantasy_ending 里依赖「动画时间」推进的逻辑
**会静默失效**——这是版本兼容的代价，换来两边都能正常加载。

顺序上不用抢：本模组 mixin 配置是默认优先级 1000，uom 的配置是 `Integer.MAX_VALUE`，
配置按优先级升序 prepare，所以本字段先合入 GeoModel，之后 uom 的 accessor 才去找它。
这个补丁挂在 `client` 段、由 `WmiscMixinPlugin` 按 `fantasy_ending` + `geckolib` 两个前置门控；
没有模块开关——它必须在类加载期就位，晚一步 uom 的 accessor 已经失败。

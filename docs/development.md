# 开发环境

## JDK

Minecraft 1.20.1 / ForgeGradle 6 只认 **Java 17**。本机可用：

```
C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot
```

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot'
.\gradlew.bat build          # 出 jar
.\gradlew.bat runClient      # 开发跑客户端
.\gradlew.bat --offline build  # 依赖全在本地缓存时用，避开网络
```

## Gradle

- wrapper：Gradle 8.8（`gradle/wrapper/gradle-wrapper.properties`，走华为云镜像）
- ForgeGradle 6 不支持 configuration cache，别开
- 不要提交 `/.gradle/`、`/build/`、`/run/`

## 版本

| 项 | 值 |
|---|---|
| Minecraft | 1.20.1 |
| 编译基线 Forge | 1.20.1-47.4.10（`gradle.properties: forge_version`） |
| 目标运行环境 | Forge 47.4.28 |
| mappings | `official` 1.20.1 |
| Java | 17 |

## 加依赖模组时

`build.gradle` 里已备好 `modrinth` / `cursemaven` / `jitpack` 仓库。要按某个模组的 API 编译，
用 `compileOnly` + `fg.deobf(...)`（开发环境是 Mojmap 名，deobf 后的 jar 才和开发环境对得上），
版本坐标从实例里的 jar 或仓库页面查。

## 约定

- 日志前缀统一 `[w_misc]`
- 每个功能一个 `WmiscFeature`，配置段自动生成
- 需要前置模组的模块，把 modid 写进 `requiredMods()`，别用 `if (ModList...)` 散落判断

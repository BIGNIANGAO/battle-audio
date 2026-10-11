# 音频自定义 / BattleAudio

这是一个面向《杀戮尖塔》一代 PC 版的 ModTheSpire + BaseMod 音频规则 mod。它提供低血量、战斗、事件、能力、卡牌和其他游戏事件的音频触发条件，并允许玩家在游戏内导入自己的音频、编辑播放片段和控制规则之间的关系。

## 公开仓库范围

本仓库只发布 BattleAudio 的原创源代码、测试和构建脚本。为避免把个人资料或第三方内容公开，仓库不包含：

- 个人音频、内置音乐和语音；
- 《杀戮尖塔》、ModTheSpire、BaseMod 或其他第三方的游戏资源、截图、JAR 和 API 文件；
- 本地配置、存档、安装目录、测试日志、旧版备份和构建产物；
- Workshop 页面草稿和与具体发布账号相关的内容。

首次运行时音频库为空。请在游戏内通过“添加候选音频”导入你拥有使用权的音频文件。游戏本体、ModTheSpire、BaseMod 和测试所需的外部依赖需要由用户自行安装；本仓库不重新分发它们。

## 目录

- `src/main/java`：mod 源码。
- `src/main/resources`：mod 元数据和 BattleAudio 自有界面文本。
- `src/test/java`：单元测试、补丁检查和可选的游戏烟测入口。
- `scripts/build.ps1`：使用本机游戏和 Workshop 依赖构建 JAR。
- `scripts/game-smoke.ps1`：在隔离目录中运行真实游戏烟测的脚本。
- `docs/BUILDING.md`：构建和测试参数。
- `docs/ARCHITECTURE.md`：模块边界和数据流。
- `THIRD_PARTY_NOTICES.md`：外部依赖说明。

## 构建

在 Windows PowerShell 中准备 Java 8 兼容编译环境、游戏目录、Workshop 依赖目录和 JDK，然后显式传入路径：

```powershell
./scripts/build.ps1 `
  -GameDir 'C:\path\to\SlayTheSpire' `
  -WorkshopDir 'C:\path\to\steamapps\workshop\content\646570' `
  -JdkDir 'C:\path\to\jdk' `
  -SkipTests
```

脚本只会从这些本地路径读取依赖，输出写入被 Git 忽略的 `build/` 和 `dist/`。完整参数和测试限制见 [构建说明](docs/BUILDING.md)。

## 安装

将本地构建生成的 `dist/BattleAudio-<version>.jar` 放进游戏的 `mods` 目录，通过 ModTheSpire 启动，并启用 BaseMod 与 BattleAudio。不要同时加载多个 BattleAudio JAR。进入游戏后，从 Mods 配置入口、顶部扬声器图标或 F10 打开设置。

## 许可证

源码采用 MIT License，见 [LICENSE](LICENSE)。这项许可只适用于本仓库中 BattleAudio 自有内容；游戏、ModTheSpire、BaseMod、FFmpeg 及用户导入的音频仍由各自权利人负责。

# 构建与测试

## 依赖

`build.ps1` 需要本机已有以下内容：

- Slay the Spire 的 `desktop-1.0.jar`；
- Workshop 中的 BaseMod 和 ModTheSpire JAR；
- 能运行 `--release 8` 的 JDK；
- 如果运行测试，需要 PATH 中的 `ffmpeg.exe` 和游戏目录中的 Java 运行时。

这些依赖不会进入 Git 仓库。脚本没有个人目录默认值，使用 `-GameDir`、`-WorkshopDir` 和 `-JdkDir` 显式指定本机位置，也可以通过对应的 `BATTLEAUDIO_*` 环境变量提供默认值。

## 构建模式

仅编译和打包：

```powershell
./scripts/build.ps1 -GameDir $game -WorkshopDir $workshop -JdkDir $jdk -SkipTests
```

完整测试还会编译 `src/test/java`，运行配置、规则、OpenAL 和 ModTheSpire 补丁检查。桌面音频烟测使用脚本生成的短 PCM 测试音调；不会读取仓库中的真实音乐。真实游戏烟测需要额外的游戏安装、Workshop 依赖和可用显示/音频环境。

`-BundleDecoder` 只适合在本机已有合规 FFmpeg 时使用。公开源代码和默认构建不会携带解码器。

## 输出

输出位于 `build/` 和 `dist/`，均被 `.gitignore` 排除。发布 JAR 应作为 GitHub Release 附件单独审核第三方许可，不要把本地 JAR、游戏资源或音频提交进源码仓库。

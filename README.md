# PICO Spatial Drum Kit

一个面向 PICO Spatial SDK 的原生空间架子鼓应用。用户可以通过双手手势握住虚拟鼓槌，穿越鼓面时根据敲击速度触发对应鼓采样，并在头显中完成手势、速度、鼓面位置和软件延迟校准。

## 主要功能

- PICO Mixed Stage 中的人体尺度八件套架子鼓
- 双手追踪、握槌识别和扫掠式鼓面穿越检测
- 完整虚拟鼓槌、力度映射及 140ms 命中反馈
- 8 个本地 CC0 WAV 鼓采样，启动时校验授权记录与 SHA-256
- Snare、Kick 和整套鼓的模拟器试听入口
- 头显内手势阈值、敲击速度和逐鼓面位置校准
- `ViewModel + UiState + Event` 状态结构，以及独立的数据仓库和用例层

## 技术栈

- Kotlin / Android SDK 35
- PICO Spatial SDK 0.13.3
- Spatial ECS、SpatialUI、Tracking Pack
- Gradle 8.x

## 构建

准备 Android Studio、Android SDK 35、Java 11，以及可用的 PICO Spatial SDK Maven 依赖后执行：

```bash
./gradlew testDebugUnitTest assembleDebug
```

Debug APK 输出到：

```text
app/build/outputs/apk/debug/app-debug.apk
```

安装和启动：

```bash
./gradlew installDebug
adb shell am start -n com.example.spatialdemo/.platform.LaunchActivity
```

设备端测试：

```bash
./gradlew connectedDebugAndroidTest
```

## 模拟器试听

鼓组按坐姿高度放置，默认水平视角下可能位于画面下方。在 PICO Emulator 中按住鼠标右键拖动视角，点击 HUD 的“试听军鼓”；展开“校准与设置”后还可以试听 Kick 或整套鼓。

模拟器可以验证模型加载、采样加载和按钮播放，但不能替代真机手势阈值、鼓面位置及声学端到端延迟校准。完整步骤见 [CALIBRATION.md](CALIBRATION.md)。

## 项目结构

```text
app/src/main/java/com/example/spatialdemo/
├── audio/          # 授权采样加载、播放和延迟遥测
├── calibration/    # 真机校准配置和持久化
├── content/        # Stage、ECS 鼓组和 HUD 附件
├── data/           # 校准仓库
├── domain/         # 鼓组模型与用例
├── interaction/    # 纯 Kotlin 敲击检测
├── tracking/       # PICO 双手追踪适配
└── ui/             # SpatialUI 状态和组件
```

## 资产与授权

- 鼓组模型：**Drum Kit** by smoj，CC BY 3.0；项目包含保留原几何的 PBR 材质精修版本。详情见 [`models/LICENSES.json`](app/src/main/assets/models/LICENSES.json)。
- 鼓采样：**Drum Kit Samples** by CM Music / CarbonMonoxideMusic，CC0 1.0。详情见 [`drums/LICENSES.json`](app/src/main/assets/drums/LICENSES.json)。

第三方资产按各自清单中的许可使用。仓库公开可见并不自动授予源代码的再许可；除上述第三方资产外，源代码版权由项目所有者保留。

## 验证状态

- 11 个 JVM 单元测试通过
- 2 个 PICO Emulator 设备端测试通过
- PICO 工作流、架构和 SpatialUI 规范检查通过
- PICO Emulator 0.13.0 稳态约 52–60 FPS

`app/src/debug/AndroidManifest.xml` 只用于规避 PICO Emulator 0.13.0 对 Mixed Stage 的 24 FPS 限帧问题；生产 Manifest 保持标准 Mixed Stage 配置。

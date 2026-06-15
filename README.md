# 时光睡眠 (Android)

时光睡眠 Android 版，对标 iOS 工程 [`iossleep`](../../iossleep)，目标是用 Kotlin +
Jetpack Compose 还原同一套界面结构、交互逻辑和数据模型，应用包名与图标与 iOS 保持一致
（`cn.cjym.timesleep`）。

本文档是完整开发计划，所有实现按本计划逐步落地。

## 1. 产品定位（与 iOS 一致）

时光睡眠是一款睡眠监测 + 助眠声音 App。核心体验：

- **睡前**：选择助眠声音、设置睡眠监测、确认麦克风和健康权限。
- **夜间**：本地采集音频，进行声音事件识别，只保存必要片段和统计结果。
- **晨间**：展示睡眠时长、声音事件时间线、噪音强度、关键片段和趋势摘要。
- **长期**：按周/月查看打鼾/咳嗽/噪音等指标变化。

首页 3 个底部 Tab：**声音 / 睡眠 / 我的**，与 iOS `MainTabView` 一致，并在底部悬浮一个
全局迷你播放器（当前播放声音 + 播放/停止 + 倒计时文字）。

## 2. 技术栈选型（iOS → Android 对照）

| 关注点 | iOS 实现 | Android 实现 |
| --- | --- | --- |
| UI / 导航 | SwiftUI + TabView | Jetpack Compose + Navigation (`NavigationBar` 3 Tab) |
| 本地声音播放 | `AVAudioPlayer`，循环播放，`mixWithOthers` | `MediaPlayer`（`isLooping = true`），前台 `Service` 承载播放与后台保活 |
| 录音 / 实时分析 | `AVAudioEngine` tap + `AVAudioRecorder` | `AudioRecord` 读取 PCM，自写 WAV 文件落盘，前台 `Service`（`FOREGROUND_SERVICE_MICROPHONE`） |
| 频谱特征提取 | Accelerate (vDSP FFT) | Kotlin 实现的 Radix-2 FFT（`AudioFeatureExtractor`），逐帧计算 RMS/峰值/分贝/过零率/频谱质心/频带占比/平坦度 |
| 声音事件分类 | `FeatureBasedSleepSoundClassifier` 规则 + YAMNet 占位 | 等价规则的 `FeatureBasedSleepSoundClassifier`（数值阈值 1:1 迁移）+ TFLite/YAMNet 占位接口 |
| 健康数据 | HealthKit（睡眠分析） | Health Connect（`SleepSessionRecord`，读/写） |
| 本地配置存储 | `UserDefaults` | Jetpack `DataStore<Preferences>` |
| 睡眠记录持久化 | `FileManager` + JSON（`SleepSessions.json`） | `filesDir` + JSON（`kotlinx.serialization`），结构与 iOS 对应 |
| 图表 | Swift Charts（Bar/Line/Point/Area/Pie） | 自绘 Compose `Canvas` 图表组件（Bar+Line 趋势图、面积图、散点图、饼图），不引入额外依赖 |
| 网络请求（登录/下载） | `URLSession` | OkHttp + kotlinx.serialization，复用同一后端 `https://www.cjym123.cn` |
| 图片加载（封面） | `Bundle` 直接读取 | Coil 从 `assets://SoundResources/...` 加载 |
| 广告 SDK（穿山甲/GroMore） | `BUAdSDK`（CocoaPods，`#if canImport` 占位） | `AppSDKManager` / `PangleAdManager` 等同名占位类，默认 `AppConstants.isAdDisabled = true`，不接入真实 AAR，预留接入点 |
| 统计 SDK（友盟） | `UMCommon`（`#if canImport` 占位） | `UMengAnalytics` 占位类，同样的 `initialize/logEvent/pageBegin/pageEnd` API，默认空实现 |
| 协议同意流程 | `PrivacyAgreementView` 全屏遮罩 | Compose `PrivacyAgreementDialog`，逻辑一致：未同意时强制展示，同意后才初始化 SDK |

技术栈：Kotlin、Jetpack Compose (Material 3)、Navigation-Compose、DataStore、
kotlinx.serialization、OkHttp、Coil、Health Connect、AudioRecord/MediaPlayer。

最低支持 **minSdk 26**（AudioRecord/前台服务/Health Connect 要求），
`targetSdk`/`compileSdk` 35。

## 3. 包名与图标

- applicationId / 包名：`cn.cjym.timesleep`（与 iOS Bundle ID 保持一致）。
- App 名称：`时光睡眠`。
- 图标：取自 iOS `Resources/Assets.xcassets/AppIcon.appiconset/AppIcon.png`
  (1024×1024)，生成 Android 所需的 `mipmap-*dpi` 前景图与自适应图标
  （`ic_launcher_foreground` + 纯色背景 `ic_launcher_background`），同时生成
  `mipmap-*/ic_launcher_round`。

## 4. 项目结构

```text
androidsleep/
├─ settings.gradle.kts
├─ build.gradle.kts
├─ gradle.properties
├─ gradle/wrapper/...
└─ app/
   ├─ build.gradle.kts
   ├─ src/main/
   │  ├─ AndroidManifest.xml
   │  ├─ assets/SoundResources/
   │  │  ├─ sounds_manifest.json
   │  │  └─ 001_夏威夷海滩/ ... 172_.../  (cover.jpg + metadata.json，
   │  │     index<=20 额外含 sound.mp3/m4a)
   │  ├─ res/ (mipmap 图标、values、themes)
   │  └─ java/cn/cjym/timesleep/
   │     ├─ App.kt                       // Application，全局单例容器
   │     ├─ MainActivity.kt
   │     ├─ data/
   │     │  ├─ model/
   │     │  │  ├─ SoundScene.kt
   │     │  │  └─ SleepModels.kt         // SleepEvent/Session/Stage/Trend...
   │     │  ├─ settings/AppSettings.kt   // DataStore
   │     │  └─ sound/SoundLibrary.kt
   │     ├─ service/
   │     │  ├─ AudioPlayerManager.kt
   │     │  ├─ SleepMonitorService.kt    // 前台 Service
   │     │  ├─ AudioFeatureExtractor.kt  // FFT
   │     │  ├─ SoundClassifier.kt
   │     │  ├─ SleepSessionStore.kt
   │     │  ├─ HealthConnectManager.kt
   │     │  ├─ CacheManager.kt
   │     │  ├─ AuthRepository.kt
   │     │  └─ sdk/
   │     │     ├─ AppSDKManager.kt
   │     │     ├─ PangleAdManager.kt
   │     │     ├─ PangleSplashAdManager.kt
   │     │     ├─ PangleRewardedVideoAdManager.kt
   │     │     └─ UMengAnalytics.kt
   │     └─ ui/
   │        ├─ AppRoot.kt                // StartupGate：协议 -> SDK -> 主框架
   │        ├─ MainTabScreen.kt          // 底部 3 Tab + 全局迷你播放器
   │        ├─ theme/
   │        ├─ shared/EmptyState.kt
   │        ├─ sounds/ (SoundHomeScreen, SoundCard, DrawFeedAdCard)
   │        ├─ sleep/ (SleepHomeScreen, SleepReportCard, charts/)
   │        └─ profile/ (ProfileScreen, LoginScreen, SetPasswordScreen,
   │                      LegalWebViewScreen, PrivacyAgreementDialog)
   └─ ...
```

## 5. 资源导入计划

来源：`D:\github\iossleep\iosSleep\iosSleep\Resources\SoundResources`
（172 个场景目录 + `sounds_manifest.json`，约 32MB）。

处理规则：

- 整体复制到 `app/src/main/assets/SoundResources/`，保持目录名、`cover.jpg`、
  `metadata.json`、`sounds_manifest.json` 不变，便于和 iOS 数据保持一致。
- `index <= 20` 的场景含 `sound.mp3`/`sound.m4a`，作为内置可直接播放资源
  （对应 iOS `isBundledAudio`）。
- `index > 20` 的场景只含封面与 metadata，运行时按需从
  `https://www.cjym123.cn/api/sounds/{id}/download` 下载并缓存到
  `context.cacheDir/Sounds/{id}.{ext}`，与 iOS `AudioPlayerService.download` 行为一致。

## 6. 模块功能规划

### 6.1 声音 Tab（SoundHomeScreen）

- 顶部栏：应用图标 + 问候语（按时间段：早上好/上午好/中午好/下午好/傍晚好/晚上好/夜深了）
  + 筛选菜单（全部/收藏/定时关闭 15/30/60 分钟/取消定时）。
- 搜索框：按标题/副标题/分类过滤。
- 分类横向标签：推荐/收藏/全部/冥想/助眠/自然/耳畔/白噪音/梦境/正念
  （关键词匹配规则与 iOS `SoundCategory` 完全一致）。
- 两列网格卡片：封面、收藏心形按钮、下载进度/下载图标、播放状态角标。
- 每 4 个卡片后插入一个广告位卡片占位（`DrawFeedAdCard`，默认隐藏，
  `isAdDisabled = true`）。
- 点击卡片：切换播放/暂停（`AudioPlayerManager.toggle`）。
- 首次进入若有数据自动播放第一个场景（与 iOS `didAutoPlayFirstScene` 一致）。
- 全局迷你播放器：封面、标题/副标题、倒计时文字、播放/暂停、停止。

### 6.2 睡眠 Tab（SleepHomeScreen）

- 顶部左右滑动两页（开始睡眠 / 睡眠报告），通过横向 `Pager` 实现，
  对应 iOS `TabView(.page)`。
- 状态卡片：封面背景（`011_春雨/cover.jpg`）+ 渐变遮罩 + “开始睡眠/结束睡眠”按钮，
  监测中显示实时环境分贝；右上角 Health Connect 授权按钮（心形图标）。
- 事件统计九宫格：打鼾/咳嗽/说梦话/磨牙/环境噪音/大口呼吸/鼻塞/放屁/憋气，
  对应 `SleepEventType`（9 种）。
- 趋势面板：周/月切换，柱状图（睡眠时长）+ 折线图（评分/12），平均评分/打鼾/磨牙汇总。
- 睡眠报告卡片（`SleepReportCard`）：
  - 睡眠评分（含与 iOS 相同的评分公式）、时长、平均/峰值噪音、睡眠效率、睡眠年龄、深睡时长。
  - 睡眠录音回放入口（激励视频解锁逻辑占位：广告关闭时直接放行）。
  - 事件散点图（时间 × 事件类型）。
  - 噪音曲线图（折线 + 面积）。
  - 睡眠分布饼图（深睡/浅睡/做梦/觉醒）+ 图例 + 文字分析。
  - 最近 20 条事件列表（类型、时间、置信度）。
- 历史报告列表：最近 10 条会话，点击切换 `latestSession`。
- 麦克风权限被拒提示对话框。

### 6.3 我的 Tab（ProfileScreen）

- 头部卡片：封面背景（`010_热带雨林/cover.jpg`）+ 登录状态/手机号 +
  “登录/注册”或“退出登录 + 修改密码”按钮。
- 权限区：麦克风状态、Health Connect 状态 + 授权按钮。
- 睡眠监测设置：保存识别片段开关、识别灵敏度滑杆（0.3~0.95）。
- 数据区：缓存大小展示、清除数据（二次确认，清空声音缓存+睡眠记录+录音文件，
  不删除内置 assets）。
- 关于区：用户协议/隐私政策（WebView 加载 `cjym123.cn` 链接）、版本号。
- 登录页（`LoginScreen`）：手机号 + 验证码/密码两种方式，调用同一后端接口。
- 修改密码页（`SetPasswordScreen`）：验证码 + 新密码。
- 隐私协议弹窗（`PrivacyAgreementDialog`）：未同意时全屏强制展示，勾选后才能继续，
  与 iOS `PrivacyAgreementView` 文案一致。

## 7. 数据模型映射

与 iOS `Models/SleepModels.swift`、`SoundScene.swift`、`AppSettings.swift` 字段
1:1 对应（详见各实现文件注释），核心公式（`sleepScore`、`sleepEfficiency`、
`sleepAge`、`SleepStageAnalyzer`、`SleepTrendCalculator`）按相同算法迁移，
保证两端报告结果一致。

## 8. 权限配置（AndroidManifest）

- `RECORD_AUDIO`：睡眠声音监测。
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MICROPHONE`：后台持续监测。
- `POST_NOTIFICATIONS`：前台服务通知（Android 13+）。
- `INTERNET`：登录、声音下载、Health Connect 不需要此权限但登录/下载需要。
- Health Connect：`android.permission.health.READ_SLEEP` /
  `android.permission.health.WRITE_SLEEP`。

## 9. 里程碑

- **M1 项目骨架**：Gradle 工程、包名 `cn.cjym.sleep`、图标、主题、3 Tab 导航
  + 全局迷你播放器骨架、空状态组件。
- **M2 资源导入**：复制 172 个声音资源目录到 `assets/SoundResources`，
  校验 manifest 可被读取。
- **M3 声音模块**：`SoundLibrary`、`AudioPlayerManager`（含下载/缓存/定时关闭）、
  `SoundHomeScreen`、`SoundCard`、收藏（DataStore）。
- **M4 睡眠模块**：`AudioFeatureExtractor`（FFT）、`SoundClassifier`、
  `SleepMonitorService`（前台服务+AudioRecord+WAV落盘）、`SleepSessionStore`、
  `SleepModels` 全量算法、图表组件、`SleepHomeScreen`/`SleepReportCard`。
- **M5 我的模块**：`AppSettings`（DataStore）、`HealthConnectManager`、
  `CacheManager`、`AuthRepository`、登录/改密/协议/隐私页面。
- **M6 SDK 占位与收尾**：`AppSDKManager` 等广告/统计占位类（默认禁用）、
  权限声明、Gradle 编译校验（`assembleDebug`）。

## 10. 验收标准

- 项目可用 Gradle 编译通过（`./gradlew :app:assembleDebug`）。
- 应用包名 `cn.cjym.sleep`，应用名“时光睡眠”，图标与 iOS 一致。
- 首页 3 个 Tab：声音、睡眠、我的，结构与交互对齐 iOS。
- 声音 Tab 可浏览/搜索/筛选/播放/收藏内置的 172 个声音场景（其中 20 个内置音频可直接播放）。
- 睡眠 Tab 可一键开始/结束监测，本地识别 9 类事件并生成包含图表的睡眠报告，
  支持周/月趋势。
- 我的 Tab 包含登录、协议/隐私、权限状态、监测设置、缓存清理。
- 广告与统计 SDK 以占位形式接入、默认关闭，不影响主流程编译与运行。

## 11. 已知简化 / 后续扩展点

- YAMNet/TFLite 模型未接入，`SoundClassifier` 仅使用与 iOS 相同的规则分类器。
- 穿山甲 GroMore / 友盟统计 Android SDK 未引入真实依赖（无 Android 侧 App
  ID/Slot ID），相关类提供与 iOS 对应的接口与日志占位，`isAdDisabled = true`。
- HealthKit → Health Connect：仅实现睡眠会话的写入与授权状态展示。

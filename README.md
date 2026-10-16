# 我的课表 · Android 版

Kotlin + Jetpack Compose 原生工程，不依赖任何 WebView，离线可用，**应用被杀死后提醒依然有效**（AlarmManager 闹钟）。

课表数据与网页版 `../index.html` 完全一致（同一份 18 门课、同一套周次算法，已交叉验证）。

## 一、最省事的出 APK 方式：Android Studio

1. 下载安装 [Android Studio](https://developer.android.com/studio)（自带 JDK 与 Android SDK，无需另外配置）
2. `File → Open`，选择本目录 `android`（**不是** 上层目录）
3. 首次打开会自动 `Gradle Sync`（需联网下载依赖，约 3-10 分钟）
   - **这一步会自动补上 `gradle/wrapper/gradle-wrapper.jar`**（本工程未附带该二进制文件，原因见文末）
4. 菜单 `Build → Build Bundle(s) / APK(s) → Build APK(s)`
5. 产物位置：`app/build/outputs/apk/debug/app-debug.apk`
6. 把这个 APK 传到手机安装（微信/QQ 发送后用手机点击安装；或数据线复制到手机后点击安装）
   - 手机需允许「安装未知来源应用」

## 二、不装 Android Studio：让 GitHub 云端帮你编译

仓库里已放好 `.github/workflows/build-apk.yml`，推到 GitHub 后会自动编译并把 APK 作为产物提供下载：

1. 在 GitHub 新建一个仓库，把本目录（`android`）内容推上去
2. 打开仓库的 **Actions** 页面，等 `Build APK` 跑完（约 3-5 分钟）
3. 在该次运行页面底部 **Artifacts** 里下载 `app-debug-apk`
4. 解压得到 `app-debug.apk`，传到手机安装

这样你电脑上什么都不用装。

## 三、已有 Gradle 的话，命令行也可以

```bash
cd android
gradle wrapper --gradle-version 8.7    # 生成缺失的 wrapper（只需一次）
./gradlew assembleDebug                # Windows 用 gradlew.bat
```

> 注意：本机若装的是 **JDK 25**，AGP 8.5 尚不支持，请改用 **JDK 17 或 21**。
> 另外命令行构建还需要 Android SDK（`ANDROID_HOME` 或 `local.properties` 里写 `sdk.dir=...`），
> 只装 JDK 是编译不了的 —— 所以推荐方案一或方案二。

## 工程结构

```
android/
├── settings.gradle.kts / build.gradle.kts / gradle.properties
├── gradle/wrapper/gradle-wrapper.properties   （jar 需首次构建生成）
├── gradlew / gradlew.bat
├── .github/workflows/build-apk.yml            （云端编译）
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/                     图标、主题、颜色、字符串
        └── java/com/example/mytimetable/
            ├── Timetable.kt         数据模型（Course / Section / Span / Timetable）
            ├── DefaultData.kt       原始课表 18 门 + 备注 + Repo（SharedPreferences 持久化）
            ├── TimetableJson.kt     课表 ⇄ JSON（用系统自带 org.json）
            ├── Reminder.kt          闹钟排程 + 通知 + 开机重排
            ├── MainActivity.kt      外壳：顶部周次条 / 底部三个 Tab / 设置页
            └── ui/
                ├── Theme.kt             Material3 主题 + 课程配色
                ├── Screens.kt           周课表网格 / 今日页 / 课程卡片
                └── EditCourseDialog.kt  编辑课程弹窗 + 课程详情弹窗
```

## 功能对照

| 功能 | 实现 |
|---|---|
| 周课表 | 7 天 × 5 大节网格，今日整列高亮，左右可滚动看周末 |
| 周次切换 | ‹ › 按钮，单独一行「回到本周」，显示日期区间 |
| 今日页 | 下一节课卡片 + 倒计时，今日课程时间轴 |
| 课前提醒 | 开关 + 提前 5/10/15/30 分钟；AlarmManager 精确闹钟，开机（BootReceiver）自动重排 |
| 编辑课表 | 新增/编辑/删除课程，改周次（点选）、单双周、教室、教师；本地 SharedPreferences 保存 |
| 恢复初始 | 一键回到从照片整理的原始课表 |
| 课程详情 | 点课程卡片，显示课程号、课堂派邀请码、教师、周次、本周是否有课 |
| 按周换老师 | 周二「通信工程专业导论」4 位老师按周次轮换，自动显示当周该来的老师 |

## 关于缺失的 `gradle-wrapper.jar`

Gradle Wrapper 的 jar 是二进制文件，本工程的生成环境**无法访问外网**（出站 TLS 被拦截），因此没有附带。
三个办法任选：

1. 用 Android Studio 打开工程 —— 它不需要这个 jar 也能同步和构建
2. 走上面的 GitHub Actions 方案
3. 本地已装 Gradle：`gradle wrapper --gradle-version 8.7`

刻意不放一个来源不明的同名二进制文件，避免给你埋下安全隐患。
